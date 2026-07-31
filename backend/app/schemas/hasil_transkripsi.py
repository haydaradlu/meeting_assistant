from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class HasilCreate(BaseModel):
    """Schema for creating a new hasil transkripsi."""
    rec_id: int
    pr_id: Optional[int] = None
    notulis_id: Optional[int] = None
    tanggal: datetime


class HasilUpdate(BaseModel):
    """Schema for updating hasil transkripsi. All fields optional."""
    hasil_transkripsi: Optional[str] = None
    hasil_rangkuman: Optional[str] = None
    status_validasi: Optional[str] = None
    summary_percentage: Optional[int] = None


class ValidateRequest(BaseModel):
    """Schema for validating a transcription result."""
    status_validasi: str


class HasilResponse(BaseModel):
    """Schema for hasil transkripsi response with relationships."""
    hasil_id: int
    rec_id: Optional[int] = None
    pr_id: Optional[int] = None
    notulis_id: Optional[int] = None
    hasil_transkripsi: Optional[str] = None
    hasil_rangkuman: Optional[str] = None
    summary_percentage: Optional[int] = 60
    tanggal: Optional[datetime] = None
    status_validasi: Optional[str] = "pending"
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    # Relationship data
    nama_rekaman: Optional[str] = None
    file_audio: Optional[str] = None
    pemimpin_rapat_name: Optional[str] = None
    notulis_name: Optional[str] = None

    class Config:
        from_attributes = True
