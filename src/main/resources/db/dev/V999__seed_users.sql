INSERT INTO usuarios (id, nombre, correo, rol_id, especialidad_id, activo) VALUES
-- COO
(X'C0000000000010008000000000000001', 'MedSync COO',    'medsynccoo@gmail.com', X'00000000000010008000000000000001', NULL,                                    true),
-- Doctor (medsync@gmail.com / medsync1) — especialidad: Medicina Interna
(X'C0000000000010008000000000000002', 'MedSync Doctor', 'medsync@gmail.com',    X'00000000000010008000000000000002', X'00000000000020008000000000000010', true);
