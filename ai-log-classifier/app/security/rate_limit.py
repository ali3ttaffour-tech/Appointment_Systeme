"""
Minimal in-memory fixed-window rate limiter, per client IP.

No external infra (Redis etc.) - appropriate for a single-replica
auxiliary service. If this service is ever scaled horizontally, replace
this with a shared store; documented as a limitation in the README.
"""

import threading
import time
from collections import defaultdict

from app.config import get_settings


class RateLimiter:
    def __init__(self):
        self._lock = threading.Lock()
        self._windows = defaultdict(lambda: (0, 0.0))  # ip -> (count, window_start)

    def allow(self, client_id: str) -> bool:
        settings = get_settings()
        if not settings.rate_limit_enabled:
            return True

        limit = settings.rate_limit_requests_per_minute
        now = time.time()
        window_seconds = 60.0

        with self._lock:
            count, window_start = self._windows[client_id]
            if now - window_start >= window_seconds:
                count, window_start = 0, now
            count += 1
            self._windows[client_id] = (count, window_start)
            return count <= limit


_limiter_singleton = RateLimiter()


def get_rate_limiter() -> RateLimiter:
    return _limiter_singleton
