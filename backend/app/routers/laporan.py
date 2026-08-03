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
import html
import re

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, HRFlowable, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY

router = APIRouter(prefix="/api/laporan", tags=["Laporan"])


def _generate_pdf_report(report_path: str, hasil: HasilTranskripsi, pr_name: Optional[str], admin_name: Optional[str]):
    doc = SimpleDocTemplate(report_path, pagesize=A4, rightMargin=50, leftMargin=50, topMargin=50, bottomMargin=50)
    styles = getSampleStyleSheet()
    
    title_style = ParagraphStyle(
        'TitleStyle',
        parent=styles['Heading1'],
        alignment=TA_CENTER,
        fontSize=16,
        leading=20,
        spaceAfter=15,
        textColor=colors.HexColor("#1A237E")
    )
    section_style = ParagraphStyle(
        'SectionStyle',
        parent=styles['Heading2'],
        fontSize=12,
        leading=16,
        spaceBefore=12,
        spaceAfter=6,
        textColor=colors.HexColor("#0D47A1")
    )
    body_style = ParagraphStyle(
        'BodyStyle',
        parent=styles['Normal'],
        fontSize=10,
        leading=14,
        alignment=TA_JUSTIFY,
        spaceAfter=8
    )
    meta_style = ParagraphStyle(
        'MetaStyle',
        parent=styles['Normal'],
        fontSize=10,
        leading=14
    )
    meta_label = ParagraphStyle(
        'MetaLabel',
        parent=styles['Normal'],
        fontSize=10,
        leading=14,
        fontName="Helvetica-Bold"
    )

    story = []
    story.append(Paragraph("LAPORAN HASIL TRANSKRIPSI RAPAT", title_style))
    story.append(HRFlowable(width="100%", thickness=2, color=colors.HexColor("#1A237E"), spaceBefore=5, spaceAfter=15))

    nama_rapat = html.escape(str(hasil.rekaman.nama_rekaman)) if (hasil.rekaman and hasil.rekaman.nama_rekaman) else "-"
    tanggal_str = html.escape(str(hasil.tanggal)) if hasil.tanggal else "-"
    status_val = hasil.status_validasi.value if hasattr(hasil.status_validasi, 'value') else str(hasil.status_validasi)
    status_str = html.escape(status_val) if hasil.status_validasi else "-"
    pr_str = html.escape(str(pr_name)) if pr_name else "-"
    admin_str = html.escape(str(admin_name)) if admin_name else "-"

    meta_data = [
        [Paragraph("Nama Rapat", meta_label), Paragraph(f": {nama_rapat}", meta_style)],
        [Paragraph("Tanggal", meta_label), Paragraph(f": {tanggal_str}", meta_style)],
        [Paragraph("Status Validasi", meta_label), Paragraph(f": {status_str}", meta_style)],
        [Paragraph("Pemimpin Rapat", meta_label), Paragraph(f": {pr_str}", meta_style)],
        [Paragraph("Admin", meta_label), Paragraph(f": {admin_str}", meta_style)],
    ]
    t = Table(meta_data, colWidths=[110, 340])
    t.setStyle(TableStyle([
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
    ]))
    story.append(t)
    story.append(Spacer(1, 15))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#B0BEC5"), spaceBefore=5, spaceAfter=15))

    story.append(Paragraph("HASIL RANGKUMAN (TEXTRANK)", section_style))
    rangkuman_text = hasil.hasil_rangkuman or "Belum ada rangkuman"
    for para in rangkuman_text.split("\n"):
        if para.strip():
            story.append(Paragraph(html.escape(para.strip()), body_style))
    story.append(Spacer(1, 10))

    story.append(Paragraph("HASIL TRANSKRIPSI LENGKAP", section_style))
    transkripsi_text = hasil.hasil_transkripsi or "Belum ada transkripsi"
    for para in transkripsi_text.split("\n"):
        if para.strip():
            story.append(Paragraph(html.escape(para.strip()), body_style))

    doc.build(story)


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
        nama_rekaman_safe = re.sub(r'[^\w\-_]', '_', str(hasil.rekaman.nama_rekaman))
        
    file_laporan = f"laporan_{nama_rekaman_safe}.pdf"

    # Save the report content to PDF file
    laporan_dir = os.path.join(settings.UPLOAD_DIR, "laporan")
    os.makedirs(laporan_dir, exist_ok=True)
    report_path = os.path.join(laporan_dir, file_laporan)

    pr = db.query(PemimpinRapat).filter(PemimpinRapat.pr_id == request.pr_id).first() if request.pr_id else None
    admin = db.query(Admin).filter(Admin.admin_id == request.admin_id).first() if request.admin_id else None

    _generate_pdf_report(
        report_path=report_path,
        hasil=hasil,
        pr_name=pr.name if pr else None,
        admin_name=admin.name if admin else None
    )

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

    laporan_dir = os.path.join(settings.UPLOAD_DIR, "laporan")
    os.makedirs(laporan_dir, exist_ok=True)
    file_path = os.path.join(laporan_dir, laporan.file_laporan)

    # If file ends with .txt or does not exist as PDF yet, auto-convert/generate PDF
    if laporan.file_laporan.endswith(".txt") or not os.path.exists(file_path):
        pdf_filename = laporan.file_laporan.replace(".txt", ".pdf") if laporan.file_laporan.endswith(".txt") else f"laporan_{laporan.laporan_id}.pdf"
        pdf_path = os.path.join(laporan_dir, pdf_filename)
        if not os.path.exists(pdf_path):
            hasil = db.query(HasilTranskripsi).filter(HasilTranskripsi.hasil_id == laporan.hasil_id).first()
            pr = db.query(PemimpinRapat).filter(PemimpinRapat.pr_id == laporan.pr_id).first() if laporan.pr_id else None
            admin = db.query(Admin).filter(Admin.admin_id == laporan.admin_id).first() if laporan.admin_id else None
            if hasil:
                _generate_pdf_report(pdf_path, hasil, pr.name if pr else None, admin.name if admin else None)
        if os.path.exists(pdf_path):
            file_path = pdf_path
            laporan.file_laporan = pdf_filename
            db.commit()

    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File laporan tidak ditemukan di server")

    return FileResponse(
        path=file_path,
        filename=laporan.file_laporan,
        media_type="application/pdf" if laporan.file_laporan.endswith(".pdf") else "application/octet-stream",
    )
