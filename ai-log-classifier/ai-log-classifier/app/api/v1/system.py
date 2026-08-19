from fastapi import APIRouter, Response

from app.services.classifier_service import get_classifier
from app.services import metrics

router = APIRouter(tags=["system"])


@router.get("/health")
def health():
    classifier = get_classifier()
    status = "UP" if classifier.is_loaded else "DEGRADED"
    return {
        "status": status,
        "model_loaded": classifier.is_loaded,
        "model_version": classifier.metadata.get("model_version", "unknown"),
    }


@router.get("/metrics")
def prometheus_metrics():
    return Response(content=metrics.prometheus_output(), media_type="text/plain")


@router.get("/api/v1/stats")
def stats():
    return {
        "recent_classifications": metrics.recent_classifications(limit=50),
    }
