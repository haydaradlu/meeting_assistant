from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class PemimpinRapatCreate(BaseModel):
    """Schema for creating a new pemimpin rapat."""
    username: str
    password: str
    name: str


class PemimpinRapatUpdate(BaseModel):
    """Schema for updating a pemimpin rapat. All fields optional."""
    username: Optional[str] = None
    password: Optional[str] = None
    name: Optional[str] = None


class PemimpinRapatResponse(BaseModel):
    """Schema for pemimpin rapat response (excludes password)."""
    pr_id: int
    username: str
    name: str
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True
