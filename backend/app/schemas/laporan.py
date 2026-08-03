from pydantic import BaseModel, field_validator
from typing import Optional
from datetime import datetime, date


class LaporanCreate(BaseModel):
    """Schema for creating a new laporan."""
    hasil_id: int
    pr_id: Optional[int] = None
    admin_id: Optional[int] = None
    tanggal_kirim: Optional[datetime] = None


class LaporanResponse(BaseModel):
    """Schema for laporan response with relationships."""
    laporan_id: int
    hasil_id: Optional[int] = None
    pr_id: Optional[int] = None
    admin_id: Optional[int] = None
    file_laporan: Optional[str] = None
    tanggal_kirim: Optional[date] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    # Relationship data
    pemimpin_rapat_name: Optional[str] = None
    admin_name: Optional[str] = None
    nama_rekaman: Optional[str] = None

    @field_validator("tanggal_kirim", mode="before")
    @classmethod
    def convert_datetime_to_date(cls, value):
        if hasattr(value, "date"):
            return value.date()
        return value

    class Config:
        from_attributes = True
