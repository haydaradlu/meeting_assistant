import os
from typing import Optional, List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.config import settings
from app.models.hasil_transkripsi import HasilTranskripsi
from app.models.rekaman_rapat import RekamanRapat
from app.models.pemimpin_rapat import PemimpinRapat
from app.models.notulis import Notulis
from app.schemas.hasil_transkripsi import HasilCreate, HasilUpdate, HasilResponse, ValidateRequest
from app.utils.auth import get_current_user, require_role
from app.schemas.auth import TokenData
from app.services.whisper_service import whisper_service
from app.services.textrank_service import textrank_service

router = APIRouter(prefix="/api/hasil", tags=["Hasil Transkripsi"])


def _build_hasil_response(hasil, db: Session) -> HasilResponse:
    """Build HasilResponse with relationship data."""
    rekaman = db.query(RekamanRapat).filter(RekamanRapat.rec_id == hasil.rec_id).first()
    pr = db.query(PemimpinRapat).filter(PemimpinRapat.pr_id == hasil.pr_id).first() if hasil.pr_id else None
    notulis = db.query(Notulis).filter(Notulis.notulis_id == hasil.notulis_id).first() if hasil.notulis_id else None

    # Use created_by from rekaman as fallback for notulis name
    notulis_display_name = None
    if notulis:
        notulis_display_name = notulis.name
    elif rekaman and rekaman.created_by:
        notulis_display_name = rekaman.created_by

    return HasilResponse(
        hasil_id=hasil.hasil_id,
        rec_id=hasil.rec_id,
        pr_id=hasil.pr_id,
        notulis_id=hasil.notulis_id,
        hasil_transkripsi=hasil.hasil_transkripsi,
        hasil_rangkuman=hasil.hasil_rangkuman,
        summary_percentage=hasil.summary_percentage,
        tanggal=hasil.tanggal,
        status_validasi=hasil.status_validasi.value if hasil.status_validasi else "pending",
        created_at=hasil.created_at,
        updated_at=hasil.updated_at,
        nama_rekaman=rekaman.nama_rekaman if rekaman else None,
        file_audio=rekaman.file_audio if rekaman else None,
        pemimpin_rapat_name=pr.name if pr else None,
        notulis_name=notulis_display_name,
    )


@router.get("/", response_model=List[HasilResponse])
def get_all_hasil(
    search: Optional[str] = None,
    status_filter: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Get all hasil transkripsi with optional search and filter."""
    query = db.query(HasilTranskripsi)

    if status_filter:
        query = query.filter(HasilTranskripsi.status_validasi == status_filter)

    results = query.order_by(HasilTranskripsi.hasil_id).all()

    response_list = []
    for hasil in results:
        resp = _build_hasil_response(hasil, db)
        # Apply search filter on nama_rekaman
        if search:
            if resp.nama_rekaman and search.lower() in resp.nama_rekaman.lower():
                response_list.append(resp)
            elif str(resp.hasil_id) == search:
                response_list.append(resp)
        else:
            response_list.append(resp)

    return response_list


@router.get("/{hasil_id}", response_model=HasilResponse)
def get_hasil(
    hasil_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Get a specific hasil transkripsi by ID with relationships."""
    hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == hasil_id).first()
    if not hasil:
        raise HTTPException(status_code=404, detail="Hasil transkripsi tidak ditemukan")

    return _build_hasil_response(hasil, db)


@router.post("/", response_model=HasilResponse, status_code=status.HTTP_201_CREATED)
def create_hasil(
    request: HasilCreate,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Create a new hasil transkripsi record."""
    # Verify rekaman exists
    rekaman = db.query(RekamanRapat).filter(RekamanRapat.rec_id == request.rec_id).first()
    if not rekaman:
        raise HTTPException(status_code=404, detail="Rekaman rapat tidak ditemukan")

    hasil = HasilTranskripsi(
        rec_id=request.rec_id,
        pr_id=request.pr_id,
        notulis_id=request.notulis_id,
        tanggal=request.tanggal,
        summary_percentage=settings.SUMMARY_PERCENTAGE,
    )
    db.add(hasil)
    db.commit()
    db.refresh(hasil)
    return _build_hasil_response(hasil, db)


@router.put("/{hasil_id}", response_model=HasilResponse)
def update_hasil(
    hasil_id: int,
    request: HasilUpdate,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Update a hasil transkripsi."""
    hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == hasil_id).first()
    if not hasil:
        raise HTTPException(status_code=404, detail="Hasil transkripsi tidak ditemukan")

    if request.hasil_transkripsi is not None:
        hasil.hasil_transkripsi = request.hasil_transkripsi
    if request.hasil_rangkuman is not None:
        hasil.hasil_rangkuman = request.hasil_rangkuman
    if request.status_validasi is not None:
        hasil.status_validasi = request.status_validasi
    if request.summary_percentage is not None:
        hasil.summary_percentage = request.summary_percentage

    db.commit()
    db.refresh(hasil)
    return _build_hasil_response(hasil, db)


@router.delete("/{hasil_id}")
def delete_hasil(
    hasil_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Delete a hasil transkripsi."""
    hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == hasil_id).first()
    if not hasil:
        raise HTTPException(status_code=404, detail="Hasil transkripsi tidak ditemukan")

    db.delete(hasil)
    db.commit()
    return {"message": "Hasil transkripsi berhasil dihapus"}


@router.post("/{hasil_id}/transcribe", response_model=HasilResponse)
def transcribe_audio(
    hasil_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Trigger Whisper transcription on the linked rekaman's audio file."""
    hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == hasil_id).first()
    if not hasil:
        raise HTTPException(status_code=404, detail="Hasil transkripsi tidak ditemukan")

    rekaman = db.query(RekamanRapat).filter(RekamanRapat.rec_id == hasil.rec_id).first()
    if not rekaman:
        raise HTTPException(status_code=404, detail="Rekaman rapat tidak ditemukan")

    audio_path = os.path.join(settings.UPLOAD_DIR, "audio", rekaman.file_audio)
    if not os.path.exists(audio_path):
        raise HTTPException(status_code=404, detail=f"File audio tidak ditemukan: {rekaman.file_audio}")

    try:
        transcript = whisper_service.transcribe(audio_path)
        hasil.hasil_transkripsi = transcript
        db.commit()
        db.refresh(hasil)
        return _build_hasil_response(hasil, db)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Gagal melakukan transkripsi: {str(e)}")


@router.post("/{hasil_id}/summarize", response_model=HasilResponse)
def summarize_transcription(
    hasil_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Trigger TextRank summarization on the transcription text."""
    hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == hasil_id).first()
    if not hasil:
        raise HTTPException(status_code=404, detail="Hasil transkripsi tidak ditemukan")

    if not hasil.hasil_transkripsi:
        raise HTTPException(status_code=400, detail="Belum ada teks transkripsi. Lakukan transkripsi terlebih dahulu.")

    try:
        percentage = hasil.summary_percentage or settings.SUMMARY_PERCENTAGE
        summary = textrank_service.summarize(hasil.hasil_transkripsi, percentage)
        hasil.hasil_rangkuman = summary
        db.commit()
        db.refresh(hasil)
        return _build_hasil_response(hasil, db)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Gagal melakukan perangkuman: {str(e)}")


@router.put("/{hasil_id}/validate", response_model=HasilResponse)
def validate_hasil(
    hasil_id: int,
    request: ValidateRequest,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Update the validation status of a hasil transkripsi."""
    hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == hasil_id).first()
    if not hasil:
        raise HTTPException(status_code=404, detail="Hasil transkripsi tidak ditemukan")

    valid_statuses = ["pending", "valid", "tidak_valid"]
    if request.status_validasi not in valid_statuses:
        raise HTTPException(
            status_code=400,
            detail=f"Status tidak valid. Pilihan: {', '.join(valid_statuses)}"
        )

    hasil.status_validasi = request.status_validasi
    db.commit()
    db.refresh(hasil)
    return _build_hasil_response(hasil, db)
