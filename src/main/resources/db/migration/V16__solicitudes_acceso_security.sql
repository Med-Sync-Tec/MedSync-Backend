-- Remove plaintext password storage (passwords never stored in DB)
ALTER TABLE solicitudes_acceso DROP COLUMN password;

-- Track when a solicitud was rejected to enforce re-registration cooldown
ALTER TABLE solicitudes_acceso ADD COLUMN rejected_at TIMESTAMP NULL;
