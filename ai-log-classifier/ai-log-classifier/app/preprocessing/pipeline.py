"""
Log preprocessing pipeline.

This module is imported by BOTH the training scripts (training/train.py)
and the inference service (app/services/classifier_service.py) so that the
exact same text transformation is applied at train time and at predict
time. Never duplicate this logic elsewhere - import from here.

The pipeline normalizes volatile/high-cardinality tokens (timestamps, IDs,
IPs, URLs, stack traces, numbers) into stable placeholders so the
classifier learns from the *shape* of a log line rather than memorizing
specific values that will never repeat in production traffic.
"""

import re

# Order matters: more specific patterns must run before more general ones
# (e.g. IPv4 before generic number, UUID before generic hex/id).

_PATTERNS = [
    # ISO-8601 / common timestamp formats
    (re.compile(r"\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:?\d{2})?"), " <TIMESTAMP> "),
    # UUIDs (also covers request IDs shaped like UUIDs)
    (re.compile(r"\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b"), " <UUID> "),
    # JWTs (three base64url segments separated by dots)
    (re.compile(r"\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b"), " <JWT> "),
    # URLs
    (re.compile(r"https?://[^\s\"'<>]+"), " <URL> "),
    # IPv4 addresses
    (re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b"), " <IP> "),
    # Email addresses
    (re.compile(r"\b[\w.+-]+@[\w-]+\.[\w.-]+\b"), " <EMAIL> "),
    # Stack trace frame lines ("at com.example.Foo.bar(Foo.java:42)")
    (re.compile(r"\bat\s+[\w.$]+\([\w.]+:\d+\)"), " <STACKFRAME> "),
    # HTTP status codes preceded by "status"/"code"/HTTP verb context are still just numbers -
    # handled by generic number normalization below, but keep 3-digit codes distinguishable
    # by normalizing them the same as other numbers (kept simple/consistent).
    # Long hex blobs (hashes, tokens) - 12+ hex chars
    (re.compile(r"\b[0-9a-fA-F]{12,}\b"), " <HEX> "),
    # Generic numeric IDs / durations / counts (do this after the more specific numeric-ish
    # patterns above so we don't clobber timestamps/hex first)
    (re.compile(r"\b\d+(\.\d+)?\b"), " <NUM> "),
]

_WHITESPACE_RE = re.compile(r"\s+")


def normalize(text: str) -> str:
    """Replace volatile tokens with stable placeholders.

    This is intentionally information-preserving at the *structural* level:
    "User 12345 failed login" -> "User <NUM> failed login" keeps the words
    that carry classification signal while dropping the specific ID that
    will never be seen again by the model.
    """
    if text is None:
        return ""

    normalized = text
    for pattern, replacement in _PATTERNS:
        normalized = pattern.sub(replacement, normalized)

    normalized = normalized.lower()
    normalized = _WHITESPACE_RE.sub(" ", normalized).strip()
    return normalized


def preprocess_for_model(text: str) -> str:
    """Full preprocessing entrypoint used right before vectorization.

    Kept as a separate function (rather than calling normalize() directly
    everywhere) so additional model-specific steps can be added later
    (e.g. stemming) in exactly one place without touching callers.
    """
    return normalize(text)
