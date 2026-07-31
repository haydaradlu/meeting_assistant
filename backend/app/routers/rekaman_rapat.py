import os
import shutil
from typing import Optional, List

from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form
from sqlalchemy.orm import Session

from app.database import get_db
from app.config import settings
from app.models.rekaman_rapat import RekamanRapat
from app.models.notulis import Notulis
from app.models.admin import Admin
from app.models.pemimpin_rapat import PemimpinRapat
from app.schemas.rekaman_rapat import RekamanResponse
from app.utils.auth import get_current_user, require_role
from app.schemas.auth import TokenData

from datetime import datetime

router = APIRouter(prefix="/api/rekaman", tags=["Rekaman Rapat"])


def _resolve_user_name(db: Session, role: str, user_id: int) -> str:
    """Resolve user name from the correct table based on role."""
    if role == "admin":
        user = db.query(Admin).filter(Admin.admin_id == user_id).first()
        return user.name if user else "Admin"
    elif role == "pemimpin_rapat":
        user = db.query(PemimpinRapat).filter(PemimpinRapat.pr_id == user_id).first()
        return user.name if user else "Pemimpin Rapat"
    elif role == "notulis":
        user = db.query(Notulis).filter(Notulis.notulis_id == user_id).first()
        return user.name if user else "Notulis"
    return "Unknown"


@router.get("/", response_model=List[RekamanResponse])
def get_all_rekaman(
    notulis_id: Optional[int] = None,
    search: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Get all rekaman rapat with optional filter by notulis_id."""
    query = db.query(RekamanRapat)
    if notulis_id:
        query = query.filter(RekamanRapat.notulis_id == notulis_id)
    if search:
        query = query.filter(RekamanRapat.nama_rekaman.ilike(f"%{search}%"))

    records = query.order_by(RekamanRapat.rec_id).all()

    result = []
    for rec in records:
        notulis = db.query(Notulis).filter(Notulis.notulis_id == rec.notulis_id).first()
        result.append(RekamanResponse(
            rec_id=rec.rec_id,
            notulis_id=rec.notulis_id,
            notulis_name=rec.created_by or (notulis.name if notulis else None),
            file_audio=rec.file_audio,
            tanggal=rec.tanggal,
            nama_rekaman=rec.nama_rekaman,
            created_by=rec.created_by,
            created_at=rec.created_at,
            updated_at=rec.updated_at,
        ))
    return result


@router.get("/{rec_id}", response_model=RekamanResponse)
def get_rekaman(
    rec_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Get a specific rekaman rapat by ID."""
    rec = db.query(RekamanRapat).filter(RekamanRapat.rec_id == rec_id).first()
    if not rec:
        raise HTTPException(status_code=404, detail="Rekaman rapat tidak ditemukan")

    notulis = db.query(Notulis).filter(Notulis.notulis_id == rec.notulis_id).first()
    return RekamanResponse(
        rec_id=rec.rec_id,
        notulis_id=rec.notulis_id,
        notulis_name=rec.created_by or (notulis.name if notulis else None),
        file_audio=rec.file_audio,
        tanggal=rec.tanggal,
        nama_rekaman=rec.nama_rekaman,
        created_by=rec.created_by,
        created_at=rec.created_at,
        updated_at=rec.updated_at,
    )


from fastapi import BackgroundTasks
from app.database import SessionLocal

def process_audio_in_background(hasil_id: int, audio_path: str):
    """Background task to run Whisper STT and TextRank summarization."""
    db = SessionLocal()
    try:
        from app.services.whisper_service import whisper_service
        from app.services.textrank_service import textrank_service
        from app.models.hasil_transkripsi import HasilTranskripsi

        print(f"[AI PIPELINE] Starting transcription for Hasil ID: {hasil_id}...")
        transcript = whisper_service.transcribe(audio_path)
        print(f"[AI PIPELINE] Transcription complete. Starting summarization...")
        
        summary = textrank_service.summarize(transcript, settings.SUMMARY_PERCENTAGE)
        print(f"[AI PIPELINE] Summarization complete.")

        hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == hasil_id).first()
        if hasil:
            hasil.hasil_transkripsi = transcript
            hasil.hasil_rangkuman = summary
            db.commit()
            print(f"[AI PIPELINE] Database updated successfully for Hasil ID: {hasil_id}!")
    except Exception as e:
        print(f"[AI PIPELINE] Error in background processing: {e}")
    finally:
        db.close()


@router.post("/", response_model=RekamanResponse, status_code=status.HTTP_201_CREATED)
async def create_rekaman(
    background_tasks: BackgroundTasks,
    nama_rekaman: str = Form(...),
    notulis_id: Optional[int] = Form(None),
    tanggal: str = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(get_current_user),
):
    """Create a new rekaman rapat with audio file upload."""
    try:
        print(f"[CREATE REKAMAN] Start - user: {current_user.username}, role: {current_user.role}, user_id: {current_user.user_id}")

        # Validate file type
        allowed_extensions = [".wav", ".mp3", ".m4a", ".ogg", ".flac", ".aac", ".wma"]
        file_ext = os.path.splitext(file.filename)[1].lower()
        if file_ext not in allowed_extensions:
            raise HTTPException(
                status_code=400,
                detail=f"Format file tidak didukung. Gunakan: {', '.join(allowed_extensions)}"
            )
        print(f"[CREATE REKAMAN] File validated: {file.filename}")

        # Save the audio file
        audio_dir = os.path.join(settings.UPLOAD_DIR, "audio")
        os.makedirs(audio_dir, exist_ok=True)

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{timestamp}_{file.filename}"
        file_path = os.path.join(audio_dir, filename)

        with open(file_path, "wb") as buffer:
            content = await file.read()
            buffer.write(content)
        print(f"[CREATE REKAMAN] File saved: {file_path}")

        # Parse tanggal
        try:
            tanggal_dt = datetime.fromisoformat(tanggal.replace("Z", "+00:00"))
        except ValueError:
            try:
                tanggal_dt = datetime.strptime(tanggal, "%Y-%m-%d")
            except ValueError:
                tanggal_dt = datetime.now()
        print(f"[CREATE REKAMAN] Tanggal parsed: {tanggal_dt}")

        # Determine notulis_id
        if notulis_id is None and current_user.role == "notulis":
            notulis_id = current_user.user_id

        # Resolve creator name from the correct table
        creator_name = _resolve_user_name(db, current_user.role, current_user.user_id)
        print(f"[CREATE REKAMAN] Creator name resolved: {creator_name}")

        rekaman = RekamanRapat(
            notulis_id=notulis_id,
            file_audio=filename,
            tanggal=tanggal_dt,
            nama_rekaman=nama_rekaman,
            created_by=creator_name,
        )
        db.add(rekaman)
        db.commit()
        db.refresh(rekaman)
        print(f"[CREATE REKAMAN] Rekaman saved with rec_id: {rekaman.rec_id}")

        # Automatically create the corresponding HasilTranskripsi record
        from app.models.hasil_transkripsi import HasilTranskripsi, StatusValidasi
        hasil = HasilTranskripsi(
            rec_id=rekaman.rec_id,
            notulis_id=rekaman.notulis_id,
            tanggal=rekaman.tanggal,
            status_validasi=StatusValidasi.pending,
            summary_percentage=settings.SUMMARY_PERCENTAGE
        )
        db.add(hasil)
        db.commit()
        db.refresh(hasil)
        print(f"[CREATE REKAMAN] HasilTranskripsi saved with hasil_id: {hasil.hasil_id}")

        # Trigger Whisper and TextRank in background
        background_tasks.add_task(process_audio_in_background, hasil.hasil_id, file_path)
        print(f"[CREATE REKAMAN] Background task queued for hasil_id: {hasil.hasil_id}")

        return RekamanResponse(
            rec_id=rekaman.rec_id,
            notulis_id=rekaman.notulis_id,
            notulis_name=creator_name,
            file_audio=rekaman.file_audio,
            tanggal=rekaman.tanggal,
            nama_rekaman=rekaman.nama_rekaman,
            created_by=creator_name,
            created_at=rekaman.created_at,
            updated_at=rekaman.updated_at,
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"[CREATE REKAMAN] ERROR: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/{rec_id}")
def delete_rekaman(
    rec_id: int,
    db: Session = Depends(get_db),
    current_user: TokenData = Depends(require_role("admin", "pemimpin_rapat")),
):
    """Delete a rekaman rapat and its audio file."""
    rec = db.query(RekamanRapat).filter(RekamanRapat.rec_id == rec_id).first()
    if not rec:
        raise HTTPException(status_code=404, detail="Rekaman rapat tidak ditemukan")

    # Delete the audio file from disk
    audio_path = os.path.join(settings.UPLOAD_DIR, "audio", rec.file_audio)
    if os.path.exists(audio_path):
        os.remove(audio_path)

    db.delete(rec)
    db.commit()
    return {"message": "Rekaman rapat berhasil dihapus"}
