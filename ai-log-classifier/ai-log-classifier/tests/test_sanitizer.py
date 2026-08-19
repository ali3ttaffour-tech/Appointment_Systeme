from app.security.sanitizer import sanitize


def test_redacts_password_field():
    result = sanitize("Login attempt password=hunter2 failed")
    assert "hunter2" not in result
    assert "<REDACTED>" in result


def test_redacts_jwt():
    token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
    result = sanitize(f"Authorization failed for token {token}")
    assert token not in result
    assert "<REDACTED_JWT>" in result


def test_redacts_bearer_header():
    result = sanitize("Authorization: Bearer abc123.def456-ghi789")
    assert "abc123.def456-ghi789" not in result


def test_preserves_non_sensitive_content():
    result = sanitize("Database connection failed for appointmentdb")
    assert result == "Database connection failed for appointmentdb"


def test_handles_empty_string():
    assert sanitize("") == ""
    assert sanitize(None) == ""
