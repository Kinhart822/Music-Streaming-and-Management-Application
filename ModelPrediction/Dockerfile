# Use an official Python runtime as a parent image
FROM python:3.12.4-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    ffmpeg \
    git \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Upgrade pip and set a longer timeout
RUN pip install --upgrade pip
RUN pip config set global.timeout 300

# Copy project files
COPY . /app

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
RUN pip install "git+https://github.com/openai/whisper.git"
RUN python -m spacy download en_core_web_sm

# Expose port 8000 for FastAPI
EXPOSE 8000

# Command to run the FastAPI application
CMD ["uvicorn", "Backend:app", "--host", "0.0.0.0", "--port", "8000"]