# ---- Stage 1: train the model -----------------------------------------
# Keeps pandas and other training-only dependencies OUT of the runtime
# image. The trained artifacts (classifier.pkl, vectorizer.pkl,
# metadata.json, evaluation reports) are the only things carried forward.
FROM python:3.12-slim AS trainer

WORKDIR /build

COPY requirements.txt requirements-training.txt ./
RUN pip install --no-cache-dir -r requirements.txt -r requirements-training.txt

COPY app ./app
COPY training ./training

RUN python training/train.py

# ---- Stage 2: runtime ---------------------------------------------------
FROM python:3.12-slim AS runtime

WORKDIR /app

RUN groupadd --system aiclassifier && useradd --system --gid aiclassifier aiclassifier

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt \
    && rm -rf /root/.cache/pip

COPY app ./app
COPY --from=trainer /build/model ./model

RUN chown -R aiclassifier:aiclassifier /app
USER aiclassifier

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=3).status==200 else 1)"

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
