from datetime import datetime, timedelta, timezone
from typing import Optional

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import JWTError, jwt
import bcrypt

from app.config import settings
from app.schemas.auth import TokenData

# HTTP Bearer security scheme
security = HTTPBearer()


def hash_password(password: str) -> str:
    """Hash a password using bcrypt.
    
    Args:
        password: Plain text password to hash.
    
    Returns:
        Bcrypt hashed password string.
    """
    pwd_bytes = password.encode('utf-8')[:72]
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(pwd_bytes, salt).decode('utf-8')


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a plain password against a bcrypt hash.
    
    Args:
        plain_password: Plain text password to verify.
        hashed_password: Bcrypt hashed password to verify against.
    
    Returns:
        True if password matches, False otherwise.
    """
    try:
        plain_bytes = plain_password.encode('utf-8')[:72]
        hash_bytes = hashed_password.encode('utf-8')
        return bcrypt.checkpw(plain_bytes, hash_bytes)
    except Exception:
        return False


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """Create a JWT access token.
    
    Args:
        data: Dictionary of claims to encode in the token.
        expires_delta: Optional custom expiration time.
    
    Returns:
        Encoded JWT token string.
    """
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=settings.JWT_EXPIRATION_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)
    return encoded_jwt


def decode_access_token(token: str) -> TokenData:
    """Decode and validate a JWT access token.
    
    Args:
        token: JWT token string to decode.
    
    Returns:
        TokenData with user_id, role, and username.
    
    Raises:
        HTTPException: If token is invalid or expired.
    """
    try:
        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
        user_id: int = payload.get("user_id")
        role: str = payload.get("role")
        username: str = payload.get("username")
        if user_id is None or role is None or username is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Token tidak valid",
                headers={"WWW-Authenticate": "Bearer"},
            )
        return TokenData(user_id=user_id, role=role, username=username)
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token tidak valid atau sudah expired",
            headers={"WWW-Authenticate": "Bearer"},
        )


def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> TokenData:
    """FastAPI dependency to get the current authenticated user from the Authorization header.
    
    Args:
        credentials: HTTP Bearer credentials extracted from the Authorization header.
    
    Returns:
        TokenData with the authenticated user's information.
    
    Raises:
        HTTPException: If no valid token is provided.
    """
    token = credentials.credentials
    return decode_access_token(token)


def require_role(*roles: str):
    """Dependency factory for role-based access control.
    
    Args:
        *roles: One or more role strings that are allowed to access the endpoint.
    
    Returns:
        A FastAPI dependency function that validates the user's role.
    
    Usage:
        @router.get("/", dependencies=[Depends(require_role("admin"))])
        or
        @router.get("/", dependencies=[Depends(require_role("admin", "pemimpin_rapat"))])
    """
    def role_checker(current_user: TokenData = Depends(get_current_user)) -> TokenData:
        if current_user.role not in roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Akses ditolak. Role yang diizinkan: {', '.join(roles)}",
            )
        return current_user
    return role_checker
