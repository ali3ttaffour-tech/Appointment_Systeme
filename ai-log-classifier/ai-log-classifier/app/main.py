import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app.config import get_settings
from app.services.classifier_service import get_classifier
from app.security.rate_limit import get_rate_limiter
from app.api.v1 import classify, system

settings = get_settings()

logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
)
logger = logging.getLogger("ai-log-classifier")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Load the model exactly once at process startup (spec section 10/24).
    get_classifier()
    yield


app = FastAPI(
    title="AI Log Classification Service",
    description=(
        "Independent, auxiliary log classification microservice for the "
        "Appointment Booking application. Classifies log lines into "
        "operational categories with severity and confidence."
    ),
    version="1.0.0",
    lifespan=lifespan,
)


class RateLimitMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        if request.url.path.startswith("/api/v1/classify"):
            client_ip = request.client.host if request.client else "unknown"
            if not get_rate_limiter().allow(client_ip):
                return JSONResponse(
                    status_code=429, content={"detail": "Rate limit exceeded. Try again shortly."}
                )
        return await call_next(request)


app.add_middleware(RateLimitMiddleware)


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    # Never expose stack traces or internal error details via the API
    # (spec section 16) - log server-side, return a generic message.
    logger.exception("Unhandled exception while processing %s", request.url.path)
    return JSONResponse(status_code=500, content={"detail": "Internal server error."})


app.include_router(system.router)
app.include_router(classify.router)
