"""Request input validation for the classification API."""

from app.config import get_settings


class ValidationError(ValueError):
    """Raised when an incoming request fails validation."""


def validate_log_text(log: str) -> str:
    settings = get_settings()

    if log is None or not isinstance(log, str):
        raise ValidationError("`log` must be a non-empty string.")

    stripped = log.strip()
    if not stripped:
        raise ValidationError("`log` must not be empty.")

    if len(log) > settings.max_log_length:
        raise ValidationError(
            f"`log` exceeds maximum allowed length of {settings.max_log_length} characters."
        )

    return log


def validate_batch(logs: list) -> list:
    settings = get_settings()

    if logs is None or not isinstance(logs, list):
        raise ValidationError("`logs` must be a non-empty array of strings.")

    if len(logs) == 0:
        raise ValidationError("`logs` must contain at least one entry.")

    if len(logs) > settings.max_batch_size:
        raise ValidationError(
            f"`logs` exceeds maximum batch size of {settings.max_batch_size}."
        )

    for entry in logs:
        validate_log_text(entry)

    return logs
