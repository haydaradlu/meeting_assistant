from pydantic import BaseModel
from typing import Optional


class LoginRequest(BaseModel):
    """Schema for login request."""
    username: str
    password: str


class LoginResponse(BaseModel):
    """Schema for login response with JWT token."""
    access_token: str
    token_type: str = "bearer"
    role: str
    user_id: int
    name: str


class TokenData(BaseModel):
    """Schema for decoded JWT token data."""
    user_id: int
    role: str
    username: str
