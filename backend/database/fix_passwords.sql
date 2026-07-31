-- Fix password hashes in the database
UPDATE admin SET password = '$2b$12$zdWkqpEINwVq04lPekXom.pEcang9s9gy.R0pOL/fHfofFklREGki';
UPDATE pemimpin_rapat SET password = '$2b$12$AxkdtfDhiHE5jMR3zWnobeut8oz7iE99ZkSIFh1MjACgUBlbhr8ry';
UPDATE notulis SET password = '$2b$12$zp.dchMGKmTKQZZoTABJ.euhsWYTmE.d0SMrk1VjNmVYa.2Qlerle';
