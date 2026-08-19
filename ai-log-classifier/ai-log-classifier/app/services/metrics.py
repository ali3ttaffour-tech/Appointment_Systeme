"""
Observability for the AI service itself (spec section 18).

Uses prometheus_client so this integrates with the app's existing
Micrometer/Prometheus stack rather than introducing a second monitoring
system. Also keeps a small in-memory recent-classifications ring buffer
that a simple dashboard endpoint can read (section 20) without needing a
database.
"""

import threading
import time
from collections import deque

from prometheus_client import Counter, Histogram, CollectorRegistry, generate_latest

registry = CollectorRegistry()

TOTAL_LOGS_PROCESSED = Counter(
    "total_logs_processed", "Total number of logs classified", registry=registry
)
CLASSIFICATION_COUNT_BY_CATEGORY = Counter(
    "classification_count_by_category",
    "Number of logs classified per category",
    ["category"],
    registry=registry,
)
CLASSIFICATION_ERRORS = Counter(
    "classification_errors", "Number of classification requests that errored", registry=registry
)
LOW_CONFIDENCE_PREDICTIONS = Counter(
    "low_confidence_predictions", "Number of predictions below the accept threshold",
    registry=registry,
)
SECURITY_ALERT_COUNT = Counter(
    "security_alert_count", "Number of logs classified as SECURITY_ALERT", registry=registry
)
INFERENCE_TIME = Histogram(
    "inference_time_seconds", "Time to classify a single log", registry=registry
)

_RECENT_MAX = 200
_recent_lock = threading.Lock()
_recent = deque(maxlen=_RECENT_MAX)


def record_result(service: str, log_preview: str, result: dict) -> None:
    TOTAL_LOGS_PROCESSED.inc()
    CLASSIFICATION_COUNT_BY_CATEGORY.labels(category=result["classification"]).inc()
    if result.get("low_confidence"):
        LOW_CONFIDENCE_PREDICTIONS.inc()
    if result["classification"] == "SECURITY_ALERT":
        SECURITY_ALERT_COUNT.inc()
    INFERENCE_TIME.observe(result["processing_time_ms"] / 1000.0)

    with _recent_lock:
        _recent.appendleft(
            {
                "timestamp": time.time(),
                "service": service,
                "log": log_preview[:200],
                "classification": result["classification"],
                "severity": result["severity"],
                "confidence": result["confidence"],
            }
        )


def record_error() -> None:
    CLASSIFICATION_ERRORS.inc()


def recent_classifications(limit: int = 50) -> list:
    with _recent_lock:
        return list(_recent)[:limit]


def prometheus_output() -> bytes:
    return generate_latest(registry)
