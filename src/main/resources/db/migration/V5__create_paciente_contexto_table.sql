CREATE TABLE paciente_contexto (
    id BINARY(16) NOT NULL,
    paciente_id BINARY(16) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    valor VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_paciente_contexto PRIMARY KEY (id),
    CONSTRAINT fk_paciente_contexto_paciente
        FOREIGN KEY (paciente_id) REFERENCES patients(id)
);

CREATE INDEX idx_paciente_contexto_paciente
    ON paciente_contexto (paciente_id);

CREATE INDEX idx_paciente_contexto_lookup
    ON paciente_contexto (paciente_id, tipo, valor);
