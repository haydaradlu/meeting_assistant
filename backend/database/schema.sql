-- ============================================
-- Meeting Assistant Database Schema
-- ============================================

-- Create custom enum type for validation status
CREATE TYPE status_validasi_enum AS ENUM ('pending', 'valid', 'tidak_valid');

-- ============================================
-- Table: admin
-- ============================================
CREATE TABLE admin (
    admin_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Table: pemimpin_rapat
-- ============================================
CREATE TABLE pemimpin_rapat (
    pr_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Table: notulis
-- ============================================
CREATE TABLE notulis (
    notulis_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Table: rekaman_rapat
-- ============================================
CREATE TABLE rekaman_rapat (
    rec_id SERIAL PRIMARY KEY,
    notulis_id INTEGER REFERENCES notulis(notulis_id) ON DELETE SET NULL,
    file_audio VARCHAR(255) NOT NULL,
    tanggal TIMESTAMP NOT NULL,
    nama_rekaman VARCHAR(100) NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Table: hasil_transkripsi
-- ============================================
CREATE TABLE hasil_transkripsi (
    hasil_id SERIAL PRIMARY KEY,
    rec_id INTEGER REFERENCES rekaman_rapat(rec_id) ON DELETE CASCADE,
    pr_id INTEGER REFERENCES pemimpin_rapat(pr_id) ON DELETE SET NULL,
    notulis_id INTEGER REFERENCES notulis(notulis_id) ON DELETE SET NULL,
    hasil_transkripsi TEXT,
    hasil_rangkuman TEXT,
    summary_percentage SMALLINT DEFAULT 60,
    tanggal TIMESTAMP NOT NULL,
    status_validasi status_validasi_enum DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Table: laporan_hasil_transkripsi
-- ============================================
CREATE TABLE laporan_hasil_transkripsi (
    laporan_id SERIAL PRIMARY KEY,
    hasil_id INTEGER REFERENCES hasil_transkripsi(hasil_id) ON DELETE CASCADE,
    pr_id INTEGER REFERENCES pemimpin_rapat(pr_id) ON DELETE SET NULL,
    admin_id INTEGER REFERENCES admin(admin_id) ON DELETE SET NULL,
    file_laporan VARCHAR(255),
    tanggal_kirim TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Indexes on foreign key columns
-- ============================================
CREATE INDEX idx_rekaman_notulis_id ON rekaman_rapat(notulis_id);
CREATE INDEX idx_hasil_rec_id ON hasil_transkripsi(rec_id);
CREATE INDEX idx_hasil_pr_id ON hasil_transkripsi(pr_id);
CREATE INDEX idx_hasil_notulis_id ON hasil_transkripsi(notulis_id);
CREATE INDEX idx_hasil_status ON hasil_transkripsi(status_validasi);
CREATE INDEX idx_laporan_hasil_id ON laporan_hasil_transkripsi(hasil_id);
CREATE INDEX idx_laporan_pr_id ON laporan_hasil_transkripsi(pr_id);
CREATE INDEX idx_laporan_admin_id ON laporan_hasil_transkripsi(admin_id);

-- ============================================
-- Trigger function for auto-updating updated_at
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables
CREATE TRIGGER trigger_admin_updated_at
    BEFORE UPDATE ON admin
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_pemimpin_rapat_updated_at
    BEFORE UPDATE ON pemimpin_rapat
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_notulis_updated_at
    BEFORE UPDATE ON notulis
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_rekaman_rapat_updated_at
    BEFORE UPDATE ON rekaman_rapat
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_hasil_transkripsi_updated_at
    BEFORE UPDATE ON hasil_transkripsi
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_laporan_updated_at
    BEFORE UPDATE ON laporan_hasil_transkripsi
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
