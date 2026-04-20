CREATE DATABASE IF NOT EXISTS hospital;
USE hospital;

CREATE TABLE IF NOT EXISTS expedientes_clinicos (
    id VARCHAR(50) NOT NULL,
    paciente_externo_id VARCHAR(100) NOT NULL,
    doctor_responsable_id VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_expedientes_clinicos PRIMARY KEY (id),
    CONSTRAINT uq_expedientes_paciente UNIQUE (paciente_externo_id)
);

CREATE INDEX idx_expedientes_paciente ON expedientes_clinicos (paciente_externo_id);

CREATE TABLE IF NOT EXISTS consultas (
    id VARCHAR(50) NOT NULL,
    expediente_id VARCHAR(50) NOT NULL,
    fecha DATETIME NOT NULL,
    motivo_consulta TEXT NULL,
    subjetivo TEXT NULL,
    objetivo TEXT NULL,
    evaluacion TEXT NULL,
    plan TEXT NULL,
    prescripcion TEXT NULL,
    diagnostico TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_consultas PRIMARY KEY (id),
    CONSTRAINT fk_consultas_expediente FOREIGN KEY (expediente_id)
        REFERENCES expedientes_clinicos(id)
);

CREATE INDEX idx_consultas_expediente ON consultas (expediente_id);
CREATE INDEX idx_consultas_fecha ON consultas (fecha);
