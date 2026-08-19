from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field
from typing import List, Optional

from app.security.validation import validate_log_text, validate_batch, ValidationError
from app.services.classifier_service import get_classifier, ModelNotLoadedError
from app.services.alerting import get_alert_aggregator
from app.services import metrics

router = APIRouter(prefix="/api/v1", tags=["classification"])


class ClassifyRequest(BaseModel):
    log: str = Field(..., description="Raw log line to classify")
    service: Optional[str] = Field(None, description="Originating service name")


class ClassifyBatchRequest(BaseModel):
    logs: List[str] = Field(..., description="Raw log lines to classify")
    service: Optional[str] = Field(None, description="Originating service name")


class ClassifyResponse(BaseModel):
    classification: str
    severity: str
    confidence: float
    low_confidence: bool
    sanitized_log: str
    model_version: str
    processing_time_ms: float
    message: str


def _run_classification(log: str, service: Optional[str]) -> dict:
    classifier = get_classifier()
    if not classifier.is_loaded:
        metrics.record_error()
        raise HTTPException(status_code=503, detail="Model is not loaded. Service unavailable.")

    try:
        result = classifier.classify(log)
    except Exception:
        # Never leak stack traces through the API (spec section 16).
        metrics.record_error()
        raise HTTPException(status_code=500, detail="Internal error while classifying log.")

    metrics.record_result(service or "unknown", log, result)
    get_alert_aggregator().record(result["classification"], result["severity"])

    result.pop("raw_predicted_classification", None)
    return result


@router.post("/classify", response_model=ClassifyResponse)
def classify(payload: ClassifyRequest, request: Request):
    try:
        validate_log_text(payload.log)
    except ValidationError as e:
        raise HTTPException(status_code=400, detail=str(e))

    return _run_classification(payload.log, payload.service)


@router.post("/classify/batch")
def classify_batch(payload: ClassifyBatchRequest, request: Request):
    try:
        validate_batch(payload.logs)
    except ValidationError as e:
        raise HTTPException(status_code=400, detail=str(e))

    results = [_run_classification(log, payload.service) for log in payload.logs]
    return {"results": results, "count": len(results)}
