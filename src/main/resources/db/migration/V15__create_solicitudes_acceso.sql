CREATE TABLE solicitudes_acceso (
    id         BINARY(16)   PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL, -- NOSONAR
    correo     VARCHAR(100) NOT NULL UNIQUE, -- NOSONAR
    password   VARCHAR(100) NOT NULL, -- NOSONAR
    rol        VARCHAR(30)  NOT NULL, -- NOSONAR
    estado     VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE', -- NOSONAR
    token      VARCHAR(36)  NOT NULL UNIQUE, -- NOSONAR
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
