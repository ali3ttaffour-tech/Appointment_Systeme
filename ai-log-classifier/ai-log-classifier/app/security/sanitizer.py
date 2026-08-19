"""
Sanitization of sensitive data from raw log text.

This is a security control, distinct from app.preprocessing.pipeline
(which normalizes text for the ML model). This module's job is to make
sure secrets never get stored, forwarded, or echoed back in an API
response - even if the upstream application accidentally sends them.

Applied BEFORE preprocessing/classification and BEFORE anything is
persisted (e.g. into `sanitized_log`).
"""

import re

_REDACTIONS = [
    # JWTs
    (re.compile(r"\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b"), "<REDACTED_JWT>"),
    # Authorization: Bearer <token> headers - must run BEFORE the generic
    # key=value rule below, otherwise that rule partially consumes
    # "Authorization: Bearer" and leaves the token itself exposed.
    (re.compile(r"(?i)\bBearer\s+[A-Za-z0-9._-]+\b"), "Bearer <REDACTED>"),
    # key=value style secrets: password, token, secret, apikey, authorization, api_key, pwd
    (
        re.compile(
            r"(?i)\b(password|passwd|pwd|token|secret|api[_-]?key|authorization|access[_-]?key)\b"
            r"\s*[:=]\s*(\"[^\"]*\"|'[^']*'|\S+)"
        ),
        r"\1=<REDACTED>",
    ),
    # Credit-card-like 13-19 digit sequences (grouped or not)
    (re.compile(r"\b(?:\d[ -]?){13,19}\b"), "<REDACTED_CC>"),
    # Basic auth style connection strings user:pass@host
    (re.compile(r"(?i)\b([a-z0-9._%+-]+):([^@\s]+)@"), r"\1:<REDACTED>@"),
]


def sanitize(text: str) -> str:
    """Redact secrets from a raw log string.

    This never removes structural/classification-relevant content (error
    types, service names, status codes) - only credential-shaped values.
    """
    if not text:
        return text or ""

    sanitized = text
    for pattern, replacement in _REDACTIONS:
        sanitized = pattern.sub(replacement, sanitized)
    return sanitized
