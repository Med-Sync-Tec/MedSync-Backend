-- Fixed UUIDs for seed roles: portable across MySQL and H2 (X'...' binary
-- literals work in both). Deterministic IDs make debugging easier and keep
-- the same role identity across environments.
INSERT INTO roles (id, nombre, descripcion) VALUES
(X'00000000000010008000000000000001', 'COO',    'Chief Operating Officer'),
(X'00000000000010008000000000000002', 'DOCTOR', 'Personal médico'),
(X'00000000000010008000000000000003', 'CMO',    'Chief Medical Officer');
