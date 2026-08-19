"""
Severity engine.

Deliberately kept separate from the ML classifier: classification answers
"what kind of log is this", severity answers "how much should an operator
care". Keeping them apart means severity policy can change (e.g. someone
decides VALIDATION_ERROR should be MEDIUM not LOW) without retraining
anything.
"""

# category -> severity. Change this table to retune business policy;
# nothing else in the codebase should hardcode a category->severity link.
_SEVERITY_MAP = {
    "INFO": "LOW",
    "WARNING": "MEDIUM",
    "PERFORMANCE_WARNING": "MEDIUM",
    "VALIDATION_ERROR": "LOW",
    "APPLICATION_ERROR": "HIGH",
    "DATABASE_ERROR": "HIGH",
    "AUTHENTICATION_ERROR": "HIGH",
    "AUTHORIZATION_ERROR": "HIGH",
    "NETWORK_ERROR": "MEDIUM",
    "SECURITY_ALERT": "CRITICAL",
    "SYSTEM_ERROR": "CRITICAL",
    "UNKNOWN": "LOW",
}

_DEFAULT_SEVERITY = "MEDIUM"


def severity_for(category: str) -> str:
    return _SEVERITY_MAP.get(category, _DEFAULT_SEVERITY)


def all_categories() -> list:
    return list(_SEVERITY_MAP.keys())
