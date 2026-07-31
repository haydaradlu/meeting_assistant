from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class AdminCreate(BaseModel):
    """Schema for creating a new admin."""
    username: str
    password: str
    name: str


class AdminUpdate(BaseModel):
    """Schema for updating an admin. All fields optional."""
    username: Optional[str] = None
    password: Optional[str] = None
    name: Optional[str] = None


class AdminResponse(BaseModel):
    """Schema for admin response (excludes password)."""
    admin_id: int
    username: str
    name: str
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True
