from typing import Optional, List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.notulis import Notulis
from app.schemas.notulis import NotulisCreate, NotulisUpdate, NotulisResponse
from app.utils.auth import get_current_user, require_role, hash_password
from app.schemas.auth import TokenData

router = APIRouter(prefix="/api/notulis", tags=["Notulis"])


@router.get("/", response_model=List[NotulisResponse])
def get_all_notulis(
    search: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Get all notulis with optional search."""
    query = db.query(Notulis)
    if search:
        query = query.filter(
            (Notulis.name.ilike(f"%{search}%")) | (Notulis.username.ilike(f"%{search}%"))
        )
    return query.order_by(Notulis.notulis_id).all()


@router.get("/{notulis_id}", response_model=NotulisResponse)
def get_notulis(
    notulis_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Get a specific notulis by ID."""
    notulis = db.query(Notulis).filter(Notulis.notulis_id == notulis_id).first()
    if not notulis:
        raise HTTPException(status_code=404, detail="Notulis tidak ditemukan")
    return notulis


@router.post("/", response_model=NotulisResponse, status_code=status.HTTP_201_CREATED)
def create_notulis(
    request: NotulisCreate,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Create a new notulis."""
    existing = db.query(Notulis).filter(Notulis.username == request.username).first()
    if existing:
        raise HTTPException(status_code=400, detail="Username sudah digunakan")

    notulis = Notulis(
        username=request.username,
        password=hash_password(request.password),
        name=request.name,
    )
    db.add(notulis)
    db.commit()
    db.refresh(notulis)
    return notulis


@router.put("/{notulis_id}", response_model=NotulisResponse)
def update_notulis(
    notulis_id: int,
    request: NotulisUpdate,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Update an existing notulis."""
    notulis = db.query(Notulis).filter(Notulis.notulis_id == notulis_id).first()
    if not notulis:
        raise HTTPException(status_code=404, detail="Notulis tidak ditemukan")

    if request.username is not None:
        existing = db.query(Notulis).filter(
            Notulis.username == request.username, Notulis.notulis_id != notulis_id
        ).first()
        if existing:
            raise HTTPException(status_code=400, detail="Username sudah digunakan")
        notulis.username = request.username

    if request.password is not None:
        notulis.password = hash_password(request.password)

    if request.name is not None:
        notulis.name = request.name

    db.commit()
    db.refresh(notulis)
    return notulis


@router.delete("/{notulis_id}")
def delete_notulis(
    notulis_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Delete a notulis."""
    if current_user.role == "notulis" and current_user.user_id == notulis_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Anda tidak dapat menghapus akun Anda sendiri"
        )

    notulis = db.query(Notulis).filter(Notulis.notulis_id == notulis_id).first()
    if not notulis:
        raise HTTPException(status_code=404, detail="Notulis tidak ditemukan")

    db.delete(notulis)
    db.commit()
    return {"message": "Notulis berhasil dihapus"}
