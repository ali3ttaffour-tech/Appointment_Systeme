from app.preprocessing.pipeline import normalize, preprocess_for_model


def test_normalizes_numeric_id():
    assert normalize("User 12345 failed login") == "user <num> failed login"


def test_normalizes_ip_address():
    result = normalize("Request from 192.168.1.10 blocked")
    assert "<ip>" in result
    assert "192.168.1.10" not in result


def test_normalizes_uuid():
    result = normalize("Request abc12345-1234-1234-1234-1234567890ab failed")
    assert "<uuid>" in result


def test_normalizes_timestamp():
    result = normalize("Event at 2026-08-18T12:30:21Z occurred")
    assert "<timestamp>" in result


def test_normalizes_url():
    result = normalize("Failed to fetch https://example.com/api/data")
    assert "<url>" in result
    assert "example.com" not in result


def test_lowercases_and_collapses_whitespace():
    assert normalize("User   LOGGED   In") == "user logged in"


def test_empty_and_none_input():
    assert normalize("") == ""
    assert normalize(None) == ""


def test_preprocess_for_model_matches_normalize():
    text = "User 999 logged in from 10.0.0.1"
    assert preprocess_for_model(text) == normalize(text)
