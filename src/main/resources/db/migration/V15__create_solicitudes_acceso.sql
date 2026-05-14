CREATE TABLE solicitudes_acceso (
    id         BINARY(16)   PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    correo     VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    rol        VARCHAR(30)  NOT NULL,
    estado     VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    token      VARCHAR(36)  NOT NULL UNIQUE,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
