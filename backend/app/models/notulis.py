from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class Notulis(Base):
    """SQLAlchemy model for the notulis table."""

    __tablename__ = "notulis"

    notulis_id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    password = Column(String(255), nullable=False)
    name = Column(String(100), nullable=False)
    created_at = Column(DateTime, server_default=func.current_timestamp())
    updated_at = Column(DateTime, server_default=func.current_timestamp(), onupdate=func.current_timestamp())

    # Relationships
    rekaman_list = relationship("RekamanRapat", back_populates="notulis")
    hasil_list = relationship("HasilTranskripsi", back_populates="notulis")
