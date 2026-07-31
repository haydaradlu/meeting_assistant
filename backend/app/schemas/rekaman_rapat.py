from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class RekamanCreate(BaseModel):
    """Schema for creating a new rekaman rapat (metadata only, file handled separately)."""
    notulis_id: Optional[int] = None
    nama_rekaman: str
    tanggal: datetime


class RekamanResponse(BaseModel):
    """Schema for rekaman rapat response."""
    rec_id: int
    notulis_id: Optional[int] = None
    notulis_name: Optional[str] = None
    file_audio: str
    tanggal: datetime
    nama_rekaman: str
    created_by: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True
