"""
Classifier service.

Loads the trained model + vectorizer exactly once (at process startup,
via get_classifier()) and reuses them for every request - never reload
per-request. See section 24/27 of the spec this service implements.
"""

import json
import logging
import os
import time

import joblib

from app.config import get_settings
from app.preprocessing.pipeline import preprocess_for_model
from app.security.sanitizer import sanitize
from app.services.severity import severity_for

logger = logging.getLogger("ai-log-classifier")


class ModelNotLoadedError(RuntimeError):
    pass


class ClassifierService:
    def __init__(self):
        settings = get_settings()
        self.model_dir = settings.model_dir
        self.model = None
        self.vectorizer = None
        self.metadata = {}
        self._loaded = False

    def load(self):
        classifier_path = os.path.join(self.model_dir, "classifier.pkl")
        vectorizer_path = os.path.join(self.model_dir, "vectorizer.pkl")
        metadata_path = os.path.join(self.model_dir, "metadata.json")

        if not (os.path.exists(classifier_path) and os.path.exists(vectorizer_path)):
            logger.error(
                "Model artifacts not found in %s. Run training/train.py first.", self.model_dir
            )
            self._loaded = False
            return

        self.model = joblib.load(classifier_path)
        self.vectorizer = joblib.load(vectorizer_path)

        if os.path.exists(metadata_path):
            with open(metadata_path) as f:
                self.metadata = json.load(f)

        settings = get_settings()
        if settings.model_version_override:
            self.metadata["model_version"] = settings.model_version_override

        self._loaded = True
        logger.info(
            "Loaded model %s (version %s) from %s",
            self.metadata.get("algorithm", "unknown"),
            self.metadata.get("model_version", "unknown"),
            self.model_dir,
        )

    @property
    def is_loaded(self) -> bool:
        return self._loaded

    def _predict_one_vector(self, vector):
        proba = self.model.predict_proba(vector)[0]
        classes = self.model.classes_
        best_idx = proba.argmax()
        return classes[best_idx], float(proba[best_idx])

    def classify(self, raw_log: str) -> dict:
        if not self._loaded:
            raise ModelNotLoadedError("Model is not loaded.")

        settings = get_settings()
        start = time.time()

        sanitized = sanitize(raw_log)
        processed = preprocess_for_model(sanitized)
        vector = self.vectorizer.transform([processed])

        predicted_label, confidence = self._predict_one_vector(vector)

        applied_label = predicted_label
        low_confidence = False
        if confidence < settings.confidence_warn_threshold:
            applied_label = "UNKNOWN"
            low_confidence = True
        elif confidence < settings.confidence_accept_threshold:
            low_confidence = True

        severity = severity_for(applied_label)
        elapsed_ms = round((time.time() - start) * 1000, 3)

        return {
            "classification": applied_label,
            "raw_predicted_classification": predicted_label,
            "severity": severity,
            "confidence": round(confidence, 4),
            "low_confidence": low_confidence,
            "sanitized_log": sanitized,
            "model_version": self.metadata.get("model_version", "unknown"),
            "processing_time_ms": elapsed_ms,
            "message": _explain(applied_label, severity),
        }

    def classify_batch(self, raw_logs: list) -> list:
        return [self.classify(log) for log in raw_logs]


def _explain(label: str, severity: str) -> str:
    explanations = {
        "INFO": "The log describes normal, expected application behavior.",
        "WARNING": "The log indicates a non-critical condition worth noting.",
        "APPLICATION_ERROR": "The log indicates an unhandled application-level error.",
        "DATABASE_ERROR": "The log appears to indicate a database connectivity or query failure.",
        "AUTHENTICATION_ERROR": "The log indicates a failure to verify identity (login/token issue).",
        "AUTHORIZATION_ERROR": "The log indicates a permission/access-control failure.",
        "VALIDATION_ERROR": "The log indicates invalid input was rejected before processing.",
        "NETWORK_ERROR": "The log indicates a network connectivity or timeout issue.",
        "PERFORMANCE_WARNING": "The log indicates degraded performance (slow response/query).",
        "SECURITY_ALERT": "The log indicates a potential security threat requiring attention.",
        "SYSTEM_ERROR": "The log indicates a critical system-level failure.",
        "UNKNOWN": "The classifier could not confidently categorize this log; manual review recommended.",
    }
    return explanations.get(label, "No explanation available for this category.")


_classifier_singleton = None


def get_classifier() -> ClassifierService:
    global _classifier_singleton
    if _classifier_singleton is None:
        _classifier_singleton = ClassifierService()
        _classifier_singleton.load()
    return _classifier_singleton
