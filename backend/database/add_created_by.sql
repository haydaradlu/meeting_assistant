-- Tambahkan kolom created_by ke tabel rekaman_rapat
ALTER TABLE rekaman_rapat ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
