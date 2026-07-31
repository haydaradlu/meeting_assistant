import os
os.environ["HF_HOME"] = "E:\\Untuk Library\\huggingface"
os.environ["SENTENCE_TRANSFORMERS_HOME"] = "E:\\Untuk Library\\sentence_transformers"
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.config import settings
from app.database import engine, Base
from app.routers import auth, admin, pemimpin_rapat, notulis, rekaman_rapat, hasil_transkripsi, laporan


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events for startup and shutdown."""
    # Startup: Create upload directory
    os.makedirs(settings.UPLOAD_DIR, exist_ok=True)
    os.makedirs(os.path.join(settings.UPLOAD_DIR, "audio"), exist_ok=True)
    os.makedirs(os.path.join(settings.UPLOAD_DIR, "laporan"), exist_ok=True)

    # Create database tables
    Base.metadata.create_all(bind=engine)

    print("Meeting Assistant API started successfully!")
    print(f"Upload directory: {settings.UPLOAD_DIR}")

    yield

    # Shutdown
    print("Meeting Assistant API shutting down...")


app = FastAPI(
    title="Meeting Assistant API",
    description="API untuk Aplikasi Meeting Assistant - Implementasi Algoritma TextRank untuk Perangkuman Otomatis Notulensi Rapat",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS middleware configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include all routers
app.include_router(auth.router)
app.include_router(admin.router)
app.include_router(pemimpin_rapat.router)
app.include_router(notulis.router)
app.include_router(rekaman_rapat.router)
app.include_router(hasil_transkripsi.router)
app.include_router(laporan.router)


@app.get("/", tags=["Root"])
def root():
    """Root endpoint returning API information."""
    return {
        "message": "Meeting Assistant API",
        "version": "1.0.0",
        "docs": "/docs",
    }
