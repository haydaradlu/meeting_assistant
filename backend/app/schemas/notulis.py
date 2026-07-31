from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class NotulisCreate(BaseModel):
    """Schema for creating a new notulis."""
    username: str
    password: str
    name: str


class NotulisUpdate(BaseModel):
    """Schema for updating a notulis. All fields optional."""
    username: Optional[str] = None
    password: Optional[str] = None
    name: Optional[str] = None


class NotulisResponse(BaseModel):
    """Schema for notulis response (excludes password)."""
    notulis_id: int
    username: str
    name: str
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True
