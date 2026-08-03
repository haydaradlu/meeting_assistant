from pydantic import BaseModel, field_validator
from typing import Optional
from datetime import datetime, date


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
    tanggal: date
    nama_rekaman: str
    created_by: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @field_validator("tanggal", mode="before")
    @classmethod
    def convert_datetime_to_date(cls, value):
        if hasattr(value, "date"):
            return value.date()
        return value

    class Config:
        from_attributes = True
