from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.admin import Admin
from app.models.pemimpin_rapat import PemimpinRapat
from app.models.notulis import Notulis
from app.schemas.auth import LoginRequest, LoginResponse
from app.utils.auth import verify_password, create_access_token

router = APIRouter(prefix="/api/auth", tags=["Authentication"])


@router.post("/login", response_model=LoginResponse)
def login(request: LoginRequest, db: Session = Depends(get_db)):
    """Authenticate a user and return a JWT token.
    
    Checks credentials against admin, pemimpin_rapat, and notulis tables
    in that order. Returns a JWT token with the user's role on success.
    
    Args:
        request: Login credentials (username and password).
        db: Database session dependency.
    
    Returns:
        LoginResponse with JWT token, role, user_id, and name.
    
    Raises:
        HTTPException 401: If credentials are invalid.
    """
    # Check admin table first
    user = db.query(Admin).filter(Admin.username == request.username).first()
    if user and verify_password(request.password, user.password):
        token = create_access_token(
            data={
                "user_id": user.admin_id,
                "role": "admin",
                "username": user.username,
            }
        )
        return LoginResponse(
            access_token=token,
            token_type="bearer",
            role="admin",
            user_id=user.admin_id,
            name=user.name,
        )

    # Check pemimpin_rapat table
    user = db.query(PemimpinRapat).filter(PemimpinRapat.username == request.username).first()
    if user and verify_password(request.password, user.password):
        token = create_access_token(
            data={
                "user_id": user.pr_id,
                "role": "pemimpin_rapat",
                "username": user.username,
            }
        )
        return LoginResponse(
            access_token=token,
            token_type="bearer",
            role="pemimpin_rapat",
            user_id=user.pr_id,
            name=user.name,
        )

    # Check notulis table
    user = db.query(Notulis).filter(Notulis.username == request.username).first()
    if user and verify_password(request.password, user.password):
        token = create_access_token(
            data={
                "user_id": user.notulis_id,
                "role": "notulis",
                "username": user.username,
            }
        )
        return LoginResponse(
            access_token=token,
            token_type="bearer",
            role="notulis",
            user_id=user.notulis_id,
            name=user.name,
        )

    # No matching user found
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Username atau password salah",
        headers={"WWW-Authenticate": "Bearer"},
    )
