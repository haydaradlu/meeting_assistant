-- ============================================
-- Seed Data for Meeting Assistant
-- ============================================

-- Insert admin users
-- Password: admin123 (bcrypt hashed)
INSERT INTO admin (username, password, name) VALUES
('admin', '$2b$12$zdWkqpEINwVq04lPekXom.pEcang9s9gy.R0pOL/fHfofFklREGki', 'Administrator'),
('admin2', '$2b$12$zdWkqpEINwVq04lPekXom.pEcang9s9gy.R0pOL/fHfofFklREGki', 'Administrator 2');

-- Insert pemimpin rapat users
-- Password: pr123 (bcrypt hashed)
INSERT INTO pemimpin_rapat (username, password, name) VALUES
('PR1', '$2b$12$AxkdtfDhiHE5jMR3zWnobeut8oz7iE99ZkSIFh1MjACgUBlbhr8ry', 'Pemimpin Rapat 1'),
('PR2', '$2b$12$AxkdtfDhiHE5jMR3zWnobeut8oz7iE99ZkSIFh1MjACgUBlbhr8ry', 'Pemimpin Rapat 2');

-- Insert notulis users
-- Password: not123 (bcrypt hashed)
INSERT INTO notulis (username, password, name) VALUES
('Notulis', '$2b$12$zp.dchMGKmTKQZZoTABJ.euhsWYTmE.d0SMrk1VjNmVYa.2Qlerle', 'Notulis 1'),
('Notulis2', '$2b$12$zp.dchMGKmTKQZZoTABJ.euhsWYTmE.d0SMrk1VjNmVYa.2Qlerle', 'Notulis 2');
