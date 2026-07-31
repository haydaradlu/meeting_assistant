from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class LaporanHasilTranskripsi(Base):
    """SQLAlchemy model for the laporan_hasil_transkripsi table."""

    __tablename__ = "laporan_hasil_transkripsi"

    laporan_id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    hasil_id = Column(Integer, ForeignKey("hasil_transkripsi.hasil_id", ondelete="CASCADE"), nullable=True, index=True)
    pr_id = Column(Integer, ForeignKey("pemimpin_rapat.pr_id", ondelete="SET NULL"), nullable=True, index=True)
    admin_id = Column(Integer, ForeignKey("admin.admin_id", ondelete="SET NULL"), nullable=True, index=True)
    file_laporan = Column(String(255), nullable=True)
    tanggal_kirim = Column(DateTime, nullable=True)
    created_at = Column(DateTime, server_default=func.current_timestamp())
    updated_at = Column(DateTime, server_default=func.current_timestamp(), onupdate=func.current_timestamp())

    # Relationships
    hasil = relationship("HasilTranskripsi", back_populates="laporan_list")
    pemimpin_rapat = relationship("PemimpinRapat")
    admin = relationship("Admin")
