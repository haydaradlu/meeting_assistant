import enum

from sqlalchemy import Column, Integer, String, Text, SmallInteger, DateTime, ForeignKey, Enum
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class StatusValidasi(str, enum.Enum):
    """Enum for validation status."""
    pending = "pending"
    valid = "valid"
    tidak_valid = "tidak_valid"


class HasilTranskripsi(Base):
    """SQLAlchemy model for the hasil_transkripsi table."""

    __tablename__ = "hasil_transkripsi"

    hasil_id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    rec_id = Column(Integer, ForeignKey("rekaman_rapat.rec_id", ondelete="CASCADE"), nullable=True, index=True)
    pr_id = Column(Integer, ForeignKey("pemimpin_rapat.pr_id", ondelete="SET NULL"), nullable=True, index=True)
    notulis_id = Column(Integer, ForeignKey("notulis.notulis_id", ondelete="SET NULL"), nullable=True, index=True)
    hasil_transkripsi = Column(Text, nullable=True)
    hasil_rangkuman = Column(Text, nullable=True)
    summary_percentage = Column(SmallInteger, default=60)
    tanggal = Column(DateTime, nullable=False)
    status_validasi = Column(
        Enum(StatusValidasi, name="status_validasi_enum", create_type=False),
        default=StatusValidasi.pending,
        server_default="pending"
    )
    created_at = Column(DateTime, server_default=func.current_timestamp())
    updated_at = Column(DateTime, server_default=func.current_timestamp(), onupdate=func.current_timestamp())

    # Relationships
    rekaman = relationship("RekamanRapat", back_populates="hasil_list")
    pemimpin_rapat = relationship("PemimpinRapat")
    notulis = relationship("Notulis", back_populates="hasil_list")
    laporan_list = relationship("LaporanHasilTranskripsi", back_populates="hasil")
