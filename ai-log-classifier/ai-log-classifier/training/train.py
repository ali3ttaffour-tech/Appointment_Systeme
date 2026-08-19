"""
Training entrypoint.

Usage:
    python training/train.py

Run from the ai-log-classifier/ directory (or with it on PYTHONPATH) so
`app.preprocessing.pipeline` resolves - training MUST use the identical
preprocessing function the inference service uses at predict time.

Produces:
    model/classifier.pkl
    model/vectorizer.pkl
    model/metadata.json
    model/evaluation_report.json
    model/evaluation_report.txt   (human-readable)
"""

import csv
import json
import os
import sys
import time
from datetime import datetime, timezone

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import MultinomialNB
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import LinearSVC
from sklearn.calibration import CalibratedClassifierCV
from sklearn.metrics import (
    accuracy_score,
    precision_recall_fscore_support,
    f1_score,
    confusion_matrix,
    classification_report,
)
import joblib

from app.preprocessing.pipeline import preprocess_for_model

DATASET_PATH = os.path.join(os.path.dirname(__file__), "dataset.csv")
MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "model")
MODEL_VERSION = "1.0.0"

# Categories where misclassification is operationally costly - tracked
# separately in the report per the project requirements.
CRITICAL_CATEGORIES = [
    "SECURITY_ALERT",
    "DATABASE_ERROR",
    "AUTHENTICATION_ERROR",
    "APPLICATION_ERROR",
]


def load_dataset(path):
    logs, labels = [], []
    with open(path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if not row.get("log") or not row.get("label"):
                continue
            logs.append(row["log"])
            labels.append(row["label"])
    if len(logs) < 100:
        raise ValueError(
            f"Dataset at {path} has only {len(logs)} rows - too small to train a "
            "meaningful classifier. Run training/generate_dataset.py or supply "
            "more real logs first."
        )
    return logs, labels


def build_candidates():
    """Candidate models to compare. All wrapped so they expose predict_proba,
    which the service needs for confidence scores.

    - LogisticRegression: strong linear baseline, natively probabilistic,
      fast to train/serve, coefficients are inspectable per-class -> easy
      to explain in a presentation.
    - MultinomialNB: classic text-classification baseline, very fast,
      useful as a sanity-check floor.
    - LinearSVC: often the strongest linear model for short, high-dimensional
      TF-IDF text, but has no native predict_proba - wrapped in
      CalibratedClassifierCV (Platt scaling) so confidence scores stay valid.
    - RandomForest: nonlinear baseline; usually not the best fit for sparse
      high-dimensional TF-IDF text but included for a fair comparison.
    """
    return {
        "LogisticRegression": LogisticRegression(
            max_iter=1000, class_weight="balanced", C=5.0
        ),
        "MultinomialNB": MultinomialNB(),
        "LinearSVC (calibrated)": CalibratedClassifierCV(
            LinearSVC(class_weight="balanced", C=1.0), cv=3
        ),
        "RandomForest": RandomForestClassifier(
            n_estimators=200, class_weight="balanced", random_state=42, n_jobs=-1
        ),
    }


def evaluate_model(name, model, X_val, y_val, labels):
    preds = model.predict(X_val)
    acc = accuracy_score(y_val, preds)
    precision, recall, f1, _ = precision_recall_fscore_support(
        y_val, preds, labels=labels, average="macro", zero_division=0
    )
    critical_f1 = f1_score(
        y_val, preds, labels=[c for c in CRITICAL_CATEGORIES if c in labels],
        average="macro", zero_division=0,
    )
    print(
        f"  {name:28s} acc={acc:.4f}  macroF1={f1:.4f}  "
        f"precision={precision:.4f}  recall={recall:.4f}  criticalF1={critical_f1:.4f}"
    )
    return {
        "accuracy": acc,
        "precision_macro": precision,
        "recall_macro": recall,
        "f1_macro": f1,
        "f1_critical_categories": critical_f1,
    }


def main():
    print(f"Loading dataset from {DATASET_PATH} ...")
    logs, labels = load_dataset(DATASET_PATH)
    print(f"Loaded {len(logs)} rows across {len(set(labels))} categories.")

    print("Preprocessing (identical pipeline to inference) ...")
    processed = [preprocess_for_model(l) for l in logs]

    # 70/15/15 train/val/test split, stratified so every class is
    # represented proportionally in each split.
    X_train, X_temp, y_train, y_temp = train_test_split(
        processed, labels, test_size=0.30, random_state=42, stratify=labels
    )
    X_val, X_test, y_val, y_test = train_test_split(
        X_temp, y_temp, test_size=0.50, random_state=42, stratify=y_temp
    )
    print(f"Split sizes -> train={len(X_train)} val={len(X_val)} test={len(X_test)}")

    print("Fitting TF-IDF vectorizer ...")
    vectorizer = TfidfVectorizer(
        ngram_range=(1, 2),
        min_df=2,
        max_df=0.9,
        sublinear_tf=True,
    )
    Xv_train = vectorizer.fit_transform(X_train)
    Xv_val = vectorizer.transform(X_val)
    Xv_test = vectorizer.transform(X_test)

    unique_labels = sorted(set(labels))

    print("\nTraining and comparing candidate models on the validation split:")
    candidates = build_candidates()
    results = {}
    fitted = {}
    t0 = time.time()
    for name, model in candidates.items():
        start = time.time()
        model.fit(Xv_train, y_train)
        fit_time = time.time() - start
        metrics = evaluate_model(name, model, Xv_val, y_val, unique_labels)
        metrics["fit_time_seconds"] = round(fit_time, 3)
        results[name] = metrics
        fitted[name] = model

    # Select best by macro F1 (not accuracy - the categories we most care
    # about getting right, like SECURITY_ALERT, are not necessarily the
    # majority class, so plain accuracy can hide poor performance on them).
    best_name = max(results, key=lambda n: results[n]["f1_macro"])
    best_model = fitted[best_name]
    print(f"\nSelected model: {best_name} (macro F1 = {results[best_name]['f1_macro']:.4f})")

    print("\nFinal evaluation on held-out test set:")
    test_preds = best_model.predict(Xv_test)
    test_acc = accuracy_score(y_test, test_preds)
    test_precision, test_recall, test_f1, _ = precision_recall_fscore_support(
        y_test, test_preds, labels=unique_labels, average="macro", zero_division=0
    )
    cm = confusion_matrix(y_test, test_preds, labels=unique_labels)
    report_text = classification_report(y_test, test_preds, labels=unique_labels, zero_division=0)
    print(report_text)

    os.makedirs(MODEL_DIR, exist_ok=True)
    joblib.dump(best_model, os.path.join(MODEL_DIR, "classifier.pkl"))
    joblib.dump(vectorizer, os.path.join(MODEL_DIR, "vectorizer.pkl"))

    metadata = {
        "model_version": MODEL_VERSION,
        "algorithm": best_name,
        "training_date": datetime.now(timezone.utc).isoformat(),
        "dataset_version": "synthetic-1.0",
        "dataset_size": len(logs),
        "categories": unique_labels,
        "accuracy": round(test_acc, 4),
        "f1_score_macro": round(test_f1, 4),
        "precision_macro": round(test_precision, 4),
        "recall_macro": round(test_recall, 4),
        "validation_comparison": {
            name: {k: (round(v, 4) if isinstance(v, float) else v) for k, v in m.items()}
            for name, m in results.items()
        },
    }
    with open(os.path.join(MODEL_DIR, "metadata.json"), "w") as f:
        json.dump(metadata, f, indent=2)

    evaluation_report = {
        "model_version": MODEL_VERSION,
        "selected_algorithm": best_name,
        "test_set_size": len(X_test),
        "test_accuracy": round(test_acc, 4),
        "test_f1_macro": round(test_f1, 4),
        "test_precision_macro": round(test_precision, 4),
        "test_recall_macro": round(test_recall, 4),
        "confusion_matrix_labels": unique_labels,
        "confusion_matrix": cm.tolist(),
        "classification_report": report_text,
        "candidate_comparison": results,
    }
    with open(os.path.join(MODEL_DIR, "evaluation_report.json"), "w") as f:
        json.dump(evaluation_report, f, indent=2)
    with open(os.path.join(MODEL_DIR, "evaluation_report.txt"), "w") as f:
        f.write(f"Model: {best_name}  (version {MODEL_VERSION})\n")
        f.write(f"Test accuracy: {test_acc:.4f}\n")
        f.write(f"Test macro F1: {test_f1:.4f}\n\n")
        f.write("Classification report:\n")
        f.write(report_text)
        f.write("\nConfusion matrix (rows=actual, cols=predicted):\n")
        f.write("labels: " + ", ".join(unique_labels) + "\n")
        for row in cm.tolist():
            f.write(str(row) + "\n")

    total_time = time.time() - t0
    print(f"\nSaved model, vectorizer, and reports to {MODEL_DIR}/  (total training time {total_time:.1f}s)")


if __name__ == "__main__":
    main()
