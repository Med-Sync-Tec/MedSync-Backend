CREATE TABLE medicamento_estados (
    id BINARY(16) NOT NULL,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    descripcion TEXT,
    CONSTRAINT pk_medicamento_estados PRIMARY KEY (id)
);

CREATE TABLE medicamentos (
    id BINARY(16) NOT NULL,
    nombre VARCHAR(150) UNIQUE NOT NULL,
    estado_id BINARY(16) NOT NULL,
    descripcion TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_medicamentos PRIMARY KEY (id),
    CONSTRAINT fk_medicamento_estado FOREIGN KEY (estado_id) REFERENCES medicamento_estados(id)
);

CREATE INDEX idx_medicamentos_estado ON medicamentos (estado_id);
