CREATE TABLE especialidades (
    id BINARY(16) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    descripcion VARCHAR(500) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_especialidades PRIMARY KEY (id),
    CONSTRAINT uq_especialidades_slug UNIQUE (slug)
);

CREATE INDEX idx_especialidades_activo ON especialidades (activo);
