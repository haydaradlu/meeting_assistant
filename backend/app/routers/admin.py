from typing import Optional, List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.admin import Admin
from app.schemas.admin import AdminCreate, AdminUpdate, AdminResponse
from app.utils.auth import get_current_user, require_role, hash_password
from app.schemas.auth import TokenData

router = APIRouter(prefix="/api/admin", tags=["Admin"])


@router.get("/", response_model=List[AdminResponse])
def get_all_admins(
    search: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin")),
):
    """Get all admins with optional search by name or username."""
    query = db.query(Admin)
    if search:
        query = query.filter(
            (Admin.name.ilike(f"%{search}%")) | (Admin.username.ilike(f"%{search}%"))
        )
    admins = query.order_by(Admin.admin_id).all()
    return admins


@router.get("/{admin_id}", response_model=AdminResponse)
def get_admin(
    admin_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin")),
):
    """Get a specific admin by ID."""
    admin = db.query(Admin).filter(Admin.admin_id == admin_id).first()
    if not admin:
        raise HTTPException(status_code=404, detail="Admin tidak ditemukan")
    return admin


@router.post("/", response_model=AdminResponse, status_code=status.HTTP_201_CREATED)
def create_admin(
    request: AdminCreate,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin")),
):
    """Create a new admin."""
    # Check if username already exists
    existing = db.query(Admin).filter(Admin.username == request.username).first()
    if existing:
        raise HTTPException(status_code=400, detail="Username sudah digunakan")

    admin = Admin(
        username=request.username,
        password=hash_password(request.password),
        name=request.name,
    )
    db.add(admin)
    db.commit()
    db.refresh(admin)
    return admin


@router.put("/{admin_id}", response_model=AdminResponse)
def update_admin(
    admin_id: int,
    request: AdminUpdate,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin")),
):
    """Update an existing admin."""
    admin = db.query(Admin).filter(Admin.admin_id == admin_id).first()
    if not admin:
        raise HTTPException(status_code=404, detail="Admin tidak ditemukan")

    if request.username is not None:
        existing = db.query(Admin).filter(
            Admin.username == request.username, Admin.admin_id != admin_id
        ).first()
        if existing:
            raise HTTPException(status_code=400, detail="Username sudah digunakan")
        admin.username = request.username

    if request.password is not None:
        admin.password = hash_password(request.password)

    if request.name is not None:
        admin.name = request.name

    db.commit()
    db.refresh(admin)
    return admin


@router.delete("/{admin_id}")
def delete_admin(
    admin_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin")),
):
    """Delete an admin."""
    if current_user.role == "admin" and current_user.user_id == admin_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Anda tidak dapat menghapus akun Anda sendiri"
        )

    admin = db.query(Admin).filter(Admin.admin_id == admin_id).first()
    if not admin:
        raise HTTPException(status_code=404, detail="Admin tidak ditemukan")

    db.delete(admin)
    db.commit()
    return {"message": "Admin berhasil dihapus"}
