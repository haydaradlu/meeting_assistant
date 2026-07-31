# ============================================
# Dockerfile for Meeting Assistant FastAPI Backend
# ============================================
FROM python:3.10-slim

# Prevent Python from writing .pyc files and enable unbuffered output
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Set cache directories for HuggingFace and SentenceTransformers inside container
ENV HF_HOME=/root/.cache/huggingface
ENV SENTENCE_TRANSFORMERS_HOME=/root/.cache/sentence_transformers

# Set working directory
WORKDIR /app

# Install system dependencies (ffmpeg is required for OpenAI Whisper audio processing)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    libsndfile1 \
    gcc \
    g++ \
    curl \
    git \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copy python dependencies list
COPY requirements.txt .

# Install Python packages
RUN pip install --no-cache-dir --upgrade pip setuptools wheel && \
    pip install --no-cache-dir -r requirements.txt

# Download required NLTK data for sentence segmentation
RUN python -c "import nltk; nltk.download('punkt'); nltk.download('punkt_tab', raise_on_error=False)"

# Copy application source code
COPY ./app ./app
COPY ./database ./database

# Create upload directories
RUN mkdir -p uploads/audio uploads/laporan

# Expose port 8000 for Uvicorn
EXPOSE 8000

# Command to run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
