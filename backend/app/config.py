from pydantic_settings import BaseSettings
from dotenv import load_dotenv

load_dotenv()


class Settings(BaseSettings):
    """Application settings loaded from .env file."""

    DATABASE_URL: str = "postgresql://postgres:postgres@localhost:5432/meeting_assistant"
    JWT_SECRET_KEY: str = "your-super-secret-key-change-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRATION_MINUTES: int = 1440
    WHISPER_MODEL: str = "small"
    WHISPER_LANGUAGE: str = "id"
    SUMMARY_PERCENTAGE: int = 60
    UPLOAD_DIR: str = "uploads"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
