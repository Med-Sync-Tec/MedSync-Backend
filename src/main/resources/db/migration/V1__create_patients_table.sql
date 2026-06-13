CREATE TABLE patients (
    id BINARY(16) NOT NULL,
    expediente_externo_id VARCHAR(100) NOT NULL, -- NOSONAR
    nombre VARCHAR(200) NOT NULL, -- NOSONAR
    fecha_nacimiento DATE NOT NULL,
    genero VARCHAR(20) NULL, -- NOSONAR
    medico_id BINARY(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT uq_patients_expediente UNIQUE (expediente_externo_id)
);

CREATE INDEX idx_patients_medico ON patients (medico_id);
CREATE INDEX idx_patients_activo ON patients (activo);
