"""
Centralized environment-variable configuration.

Every tunable in this service is read from the environment so nothing is
hardcoded and behavior can change per-deployment without a rebuild. See
.env.example for the full list with defaults.
"""

import os
from dataclasses import dataclass, field
from functools import lru_cache


def _env_float(name: str, default: float) -> float:
    return float(os.environ.get(name, default))


def _env_int(name: str, default: int) -> int:
    return int(os.environ.get(name, default))


def _env_bool(name: str, default: bool) -> bool:
    val = os.environ.get(name)
    if val is None:
        return default
    return val.strip().lower() in ("1", "true", "yes", "on")


@dataclass(frozen=True)
class Settings:
    # Service
    service_name: str = os.environ.get("AI_SERVICE_NAME", "ai-log-classifier")
    port: int = field(default_factory=lambda: _env_int("AI_SERVICE_PORT", 8000))
    log_level: str = os.environ.get("LOG_LEVEL", "INFO")

    # Model
    model_dir: str = os.environ.get("MODEL_PATH", "/app/model")
    model_version_override: str = os.environ.get("MODEL_VERSION", "")

    # Confidence thresholds
    confidence_accept_threshold: float = field(
        default_factory=lambda: _env_float("CONFIDENCE_ACCEPT_THRESHOLD", 0.80)
    )
    confidence_warn_threshold: float = field(
        default_factory=lambda: _env_float("CONFIDENCE_WARN_THRESHOLD", 0.50)
    )

    # Request limits
    max_log_length: int = field(default_factory=lambda: _env_int("MAX_LOG_LENGTH", 4000))
    max_batch_size: int = field(default_factory=lambda: _env_int("MAX_BATCH_SIZE", 200))

    # Rate limiting (simple in-memory token bucket, per client IP)
    rate_limit_enabled: bool = field(default_factory=lambda: _env_bool("RATE_LIMIT_ENABLED", True))
    rate_limit_requests_per_minute: int = field(
        default_factory=lambda: _env_int("RATE_LIMIT_RPM", 300)
    )

    # Alerting thresholds (aggregation window in seconds)
    alert_window_seconds: int = field(default_factory=lambda: _env_int("ALERT_WINDOW_SECONDS", 300))
    alert_security_alert_count: int = field(
        default_factory=lambda: _env_int("ALERT_SECURITY_ALERT_COUNT", 10)
    )
    alert_database_error_count: int = field(
        default_factory=lambda: _env_int("ALERT_DATABASE_ERROR_COUNT", 15)
    )
    alert_auth_error_count: int = field(
        default_factory=lambda: _env_int("ALERT_AUTH_ERROR_COUNT", 10)
    )
    alert_application_error_count: int = field(
        default_factory=lambda: _env_int("ALERT_APPLICATION_ERROR_COUNT", 20)
    )
    alert_webhook_url: str = os.environ.get("ALERT_WEBHOOK_URL", "")


@lru_cache
def get_settings() -> Settings:
    return Settings()
