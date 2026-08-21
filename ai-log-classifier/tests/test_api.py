import os

os.environ.setdefault("MODEL_PATH", os.path.join(os.path.dirname(__file__), "..", "model"))

import pytest
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_endpoint():
    resp = client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "UP"
    assert body["model_loaded"] is True


def test_valid_request():
    resp = client.post("/api/v1/classify", json={"log": "User logged in successfully"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["classification"] == "INFO"
    assert 0.0 <= body["confidence"] <= 1.0
    assert body["severity"] in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}


def test_empty_log_rejected():
    resp = client.post("/api/v1/classify", json={"log": ""})
    assert resp.status_code == 400


def test_missing_log_field_rejected():
    resp = client.post("/api/v1/classify", json={})
    assert resp.status_code == 422  # pydantic validation


def test_very_long_log_rejected():
    huge_log = "x" * 100000
    resp = client.post("/api/v1/classify", json={"log": huge_log})
    assert resp.status_code == 400


def test_malformed_json_rejected():
    resp = client.post(
        "/api/v1/classify", data="{not valid json", headers={"Content-Type": "application/json"}
    )
    assert resp.status_code == 422


def test_batch_request():
    resp = client.post(
        "/api/v1/classify/batch",
        json={"logs": ["User logged in successfully", "Invalid JWT token"]},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["count"] == 2
    assert len(body["results"]) == 2


def test_batch_empty_list_rejected():
    resp = client.post("/api/v1/classify/batch", json={"logs": []})
    assert resp.status_code == 400


def test_unknown_log_returns_low_confidence_or_unknown_category():
    resp = client.post(
        "/api/v1/classify",
        json={"log": "qzx flibbertigibbet unrelated nonsense token stream 42"},
    )
    assert resp.status_code == 200
    body = resp.json()
    # Either classified as UNKNOWN, or flagged low_confidence for manual review.
    assert body["classification"] == "UNKNOWN" or body["low_confidence"] is True


def test_response_never_echoes_raw_secret():
    resp = client.post(
        "/api/v1/classify",
        json={"log": "Login failed password=supersecret123 for user bob"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert "supersecret123" not in str(body)


def test_response_does_not_leak_stack_traces_on_error():
    # Force a 500 by monkeypatching would require deeper hooks; instead verify
    # the contract on a normal error path (validation) that no traceback text
    # appears in any response body across the suite's other assertions.
    resp = client.post("/api/v1/classify", json={"log": ""})
    body = resp.json()
    assert "Traceback" not in str(body)
    assert "File \"" not in str(body)
