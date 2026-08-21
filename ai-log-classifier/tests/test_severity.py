from app.services.severity import severity_for, all_categories


def test_security_alert_is_critical():
    assert severity_for("SECURITY_ALERT") == "CRITICAL"


def test_info_is_low():
    assert severity_for("INFO") == "LOW"


def test_database_error_is_high():
    assert severity_for("DATABASE_ERROR") == "HIGH"


def test_unknown_category_gets_default_severity():
    assert severity_for("SOME_MADE_UP_CATEGORY") == "MEDIUM"


def test_all_categories_have_a_mapping():
    for category in all_categories():
        assert severity_for(category) in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
