import os
from typing import Optional, List

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.database import get_db
from app.config import settings
from app.models.laporan import LaporanHasilTranskripsi
from app.models.hasil_transkripsi import HasilTranskripsi
from app.models.pemimpin_rapat import PemimpinRapat
from app.models.admin import Admin
from app.schemas.laporan import LaporanCreate, LaporanResponse
from app.utils.auth import get_current_user
from app.schemas.auth import TokenData

from datetime import datetime

router = APIRouter(prefix="/api/laporan", tags=["Laporan"])


def _build_laporan_response(laporan, db: Session) -> LaporanResponse:
    """Build LaporanResponse with relationship data."""
    hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == laporan.hasil_id).first()
    pr = db.query(PemimpinRapat).filter(PemimpinRapat.pr_id == laporan.pr_id).first() if laporan.pr_id else None
    admin = db.query(Admin).filter(Admin.admin_id == laporan.admin_id).first() if laporan.admin_id else None

    return LaporanResponse(
        laporan_id=laporan.laporan_id,
        hasil_id=laporan.hasil_id,
        pr_id=laporan.pr_id,
        admin_id=laporan.admin_id,
        file_laporan=laporan.file_laporan,
        tanggal_kirim=laporan.tanggal_kirim,
        created_at=laporan.created_at,
        updated_at=laporan.updated_at,
        pemimpin_rapat_name=pr.name if pr else None,
        admin_name=admin.name if admin else None,
        nama_rekaman=None,
    )


@router.get("/", response_model=List[LaporanResponse])
def get_all_laporan(
    search: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Get all laporan hasil transkripsi."""
    query = db.query(LaporanHasilTranskripsi)
    records = query.order_by(LaporanHasilTranskripsi.laporan_id).all()

    result = []
    for laporan in records:
        resp = _build_laporan_response(laporan, db)
        if search:
            if str(resp.laporan_id) == search or (resp.file_laporan and search.lower() in resp.file_laporan.lower()):
                result.append(resp)
        else:
            result.append(resp)
    return result


@router.get("/{laporan_id}", response_model=LaporanResponse)
def get_laporan(
    laporan_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Get a specific laporan by ID."""
    laporan = db.query(LaporanHasilTranskripsi).filter(
        LaporanHasilTranskripsi.laporan_id == laporan_id
    ).first()
    if not laporan:
        raise HTTPException(status_code=404, detail="Laporan tidak ditemukan")
    return _build_laporan_response(laporan, db)


@router.post("/", response_model=LaporanResponse, status_code=status.HTTP_201_CREATED)
def create_laporan(
    request: LaporanCreate,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Create a new laporan hasil transkripsi."""
    # Verify hasil exists
    hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == request.hasil_id).first()
    if not hasil:
        raise HTTPException(status_code=404, detail="Hasil transkripsi tidak ditemukan")

    # Generate a report filename
    nama_rekaman_safe = "tanpa_nama"
    if hasil.rekaman and hasil.rekaman.nama_rekaman:
        nama_rekaman_safe = hasil.rekaman.nama_rekaman.replace(" ", "_")
        
    file_laporan = f"laporan_{nama_rekaman_safe}.txt"

    # Save the report content to file
    laporan_dir = os.path.join(settings.UPLOAD_DIR, "laporan")
    os.makedirs(laporan_dir, exist_ok=True)
    report_path = os.path.join(laporan_dir, file_laporan)

    with open(report_path, "w", encoding="utf-8") as f:
        f.write("=" * 60 + "\n")
        f.write("LAPORAN HASIL TRANSKRIPSI RAPAT\n")
        f.write("=" * 60 + "\n\n")
        f.write(f"Tanggal: {hasil.tanggal}\n")
        f.write(f"Status: {hasil.status_validasi}\n\n")
        f.write("-" * 40 + "\n")
        f.write("HASIL TRANSKRIPSI:\n")
        f.write("-" * 40 + "\n")
        f.write(f"{hasil.hasil_transkripsi or 'Belum ada transkripsi'}\n\n")
        f.write("-" * 40 + "\n")
        f.write("HASIL RANGKUMAN:\n")
        f.write("-" * 40 + "\n")
        f.write(f"{hasil.hasil_rangkuman or 'Belum ada rangkuman'}\n")
        f.write("\n" + "=" * 60 + "\n")

    # Parse tanggal_kirim
    tanggal_kirim = request.tanggal_kirim if request.tanggal_kirim else datetime.now()

    laporan = LaporanHasilTranskripsi(
        hasil_id=request.hasil_id,
        pr_id=request.pr_id,
        admin_id=request.admin_id,
        file_laporan=file_laporan,
        tanggal_kirim=tanggal_kirim,
    )
    db.add(laporan)
    db.commit()
    db.refresh(laporan)
    return _build_laporan_response(laporan, db)


@router.delete("/{laporan_id}")
def delete_laporan(
    laporan_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Delete a laporan."""
    laporan = db.query(LaporanHasilTranskripsi).filter(
        LaporanHasilTranskripsi.laporan_id == laporan_id
    ).first()
    if not laporan:
        raise HTTPException(status_code=404, detail="Laporan tidak ditemukan")

    # Delete file from disk
    if laporan.file_laporan:
        file_path = os.path.join(settings.UPLOAD_DIR, "laporan", laporan.file_laporan)
        if os.path.exists(file_path):
            os.remove(file_path)

    db.delete(laporan)
    db.commit()
    return {"message": "Laporan berhasil dihapus"}


@router.get("/{laporan_id}/download")
def download_laporan(
    laporan_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Download a laporan file."""
    laporan = db.query(LaporanHasilTranskripsi).filter(
        LaporanHasilTranskripsi.laporan_id == laporan_id
    ).first()
    if not laporan:
        raise HTTPException(status_code=404, detail="Laporan tidak ditemukan")

    if not laporan.file_laporan:
        raise HTTPException(status_code=404, detail="File laporan tidak tersedia")

    file_path = os.path.join(settings.UPLOAD_DIR, "laporan", laporan.file_laporan)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File laporan tidak ditemukan di server")

    return FileResponse(
        path=file_path,
        filename=laporan.file_laporan,
        media_type="application/octet-stream",
    )
