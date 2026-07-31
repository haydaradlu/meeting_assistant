from typing import Optional, List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.pemimpin_rapat import PemimpinRapat
from app.schemas.pemimpin_rapat import PemimpinRapatCreate, PemimpinRapatUpdate, PemimpinRapatResponse
from app.utils.auth import get_current_user, require_role, hash_password
from app.schemas.auth import TokenData

router = APIRouter(prefix="/api/pemimpin-rapat", tags=["Pemimpin Rapat"])


@router.get("/", response_model=List[PemimpinRapatResponse])
def get_all_pemimpin_rapat(
    search: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Get all pemimpin rapat with optional search."""
    query = db.query(PemimpinRapat)
    if search:
        query = query.filter(
            (PemimpinRapat.name.ilike(f"%{search}%")) | (PemimpinRapat.username.ilike(f"%{search}%"))
        )
    return query.order_by(PemimpinRapat.pr_id).all()


@router.get("/{pr_id}", response_model=PemimpinRapatResponse)
def get_pemimpin_rapat(
    pr_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Get a specific pemimpin rapat by ID."""
    pr = db.query(PemimpinRapat).filter(PemimpinRapat.pr_id == pr_id).first()
    if not pr:
        raise HTTPException(status_code=404, detail="Pemimpin Rapat tidak ditemukan")
    return pr


@router.post("/", response_model=PemimpinRapatResponse, status_code=status.HTTP_201_CREATED)
def create_pemimpin_rapat(
    request: PemimpinRapatCreate,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Create a new pemimpin rapat."""
    existing = db.query(PemimpinRapat).filter(PemimpinRapat.username == request.username).first()
    if existing:
        raise HTTPException(status_code=400, detail="Username sudah digunakan")

    pr = PemimpinRapat(
        username=request.username,
        password=hash_password(request.password),
        name=request.name,
    )
    db.add(pr)
    db.commit()
    db.refresh(pr)
    return pr


@router.put("/{pr_id}", response_model=PemimpinRapatResponse)
def update_pemimpin_rapat(
    pr_id: int,
    request: PemimpinRapatUpdate,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Update an existing pemimpin rapat."""
    pr = db.query(PemimpinRapat).filter(PemimpinRapat.pr_id == pr_id).first()
    if not pr:
        raise HTTPException(status_code=404, detail="Pemimpin Rapat tidak ditemukan")

    if request.username is not None:
        existing = db.query(PemimpinRapat).filter(
            PemimpinRapat.username == request.username, PemimpinRapat.pr_id != pr_id
        ).first()
        if existing:
            raise HTTPException(status_code=400, detail="Username sudah digunakan")
        pr.username = request.username

    if request.password is not None:
        pr.password = hash_password(request.password)

    if request.name is not None:
        pr.name = request.name

    db.commit()
    db.refresh(pr)
    return pr


@router.delete("/{pr_id}")
def delete_pemimpin_rapat(
    pr_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Delete a pemimpin rapat."""
    if current_user.role == "pemimpin_rapat" and current_user.user_id == pr_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Anda tidak dapat menghapus akun Anda sendiri"
        )

    pr = db.query(PemimpinRapat).filter(PemimpinRapat.pr_id == pr_id).first()
    if not pr:
        raise HTTPException(status_code=404, detail="Pemimpin Rapat tidak ditemukan")

    db.delete(pr)
    db.commit()
    return {"message": "Pemimpin Rapat berhasil dihapus"}
