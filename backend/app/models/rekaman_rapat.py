from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class RekamanRapat(Base):
    """SQLAlchemy model for the rekaman_rapat table."""

    __tablename__ = "rekaman_rapat"

    rec_id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    notulis_id = Column(Integer, ForeignKey("notulis.notulis_id", ondelete="SET NULL"), nullable=True, index=True)
    file_audio = Column(String(255), nullable=False)
    tanggal = Column(DateTime, nullable=False)
    nama_rekaman = Column(String(100), nullable=False)
    created_by = Column(String(100), nullable=True)
    created_at = Column(DateTime, server_default=func.current_timestamp())
    updated_at = Column(DateTime, server_default=func.current_timestamp(), onupdate=func.current_timestamp())

    # Relationships
    notulis = relationship("Notulis", back_populates="rekaman_list")
    hasil_list = relationship("HasilTranskripsi", back_populates="rekaman")
