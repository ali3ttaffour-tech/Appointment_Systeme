"""
Alert aggregation.

Deliberately simple and in-memory: this is an auxiliary observability
feature (see spec section 25 - the AI service is never allowed to become
a hard dependency of the main application), so it does not need a message
broker or external store for a project at this scale. Swap the
_notify() implementation for a real webhook/PagerDuty/Slack call later
without touching the aggregation logic.

Debouncing: rather than alerting on every single matching log, we count
occurrences in a rolling time window per category and fire at most one
alert per (category, window) once the configured threshold is crossed.
"""

import logging
import threading
import time
from collections import defaultdict, deque

from app.config import get_settings

logger = logging.getLogger("ai-log-classifier.alerting")

_ALERT_TRIGGER_CATEGORIES = {
    "SECURITY_ALERT",
    "DATABASE_ERROR",
    "AUTHENTICATION_ERROR",
    "APPLICATION_ERROR",
}


class AlertAggregator:
    def __init__(self):
        self._lock = threading.Lock()
        self._events = defaultdict(deque)  # category -> deque[timestamp]
        self._last_alert_fired = {}  # category -> timestamp of last fired alert

    def _threshold_for(self, category: str) -> int:
        settings = get_settings()
        return {
            "SECURITY_ALERT": settings.alert_security_alert_count,
            "DATABASE_ERROR": settings.alert_database_error_count,
            "AUTHENTICATION_ERROR": settings.alert_auth_error_count,
            "APPLICATION_ERROR": settings.alert_application_error_count,
        }.get(category, 0)

    def record(self, category: str, severity: str) -> None:
        if category not in _ALERT_TRIGGER_CATEGORIES and severity != "CRITICAL":
            return

        settings = get_settings()
        now = time.time()
        window = settings.alert_window_seconds

        with self._lock:
            events = self._events[category]
            events.append(now)
            # drop anything outside the rolling window
            while events and now - events[0] > window:
                events.popleft()

            threshold = self._threshold_for(category)
            if threshold <= 0:
                return

            if len(events) >= threshold:
                last_fired = self._last_alert_fired.get(category, 0)
                # debounce: only one alert per window per category
                if now - last_fired >= window:
                    self._last_alert_fired[category] = now
                    self._notify(category, len(events), window)

    def _notify(self, category: str, count: int, window_seconds: int) -> None:
        message = (
            f"ALERT: {count} '{category}' events in the last {window_seconds}s "
            "exceeded the configured threshold."
        )
        logger.warning(message)

        settings = get_settings()
        if settings.alert_webhook_url:
            try:
                import urllib.request

                payload = f'{{"text": "{message}"}}'.encode("utf-8")
                req = urllib.request.Request(
                    settings.alert_webhook_url,
                    data=payload,
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
                urllib.request.urlopen(req, timeout=2)
            except Exception as exc:  # never let alert delivery break request handling
                logger.warning("Failed to deliver alert webhook: %s", exc)


_aggregator_singleton = None


def get_alert_aggregator() -> AlertAggregator:
    global _aggregator_singleton
    if _aggregator_singleton is None:
        _aggregator_singleton = AlertAggregator()
    return _aggregator_singleton
