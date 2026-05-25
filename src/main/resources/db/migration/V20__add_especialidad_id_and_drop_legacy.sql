-- Drops the legacy free-text usuarios.especialidad column (added in V9)
-- and replaces it across the schema with a proper FK to especialidades(id).
-- The new column is NULLABLE on all three tables so existing rows remain valid.
--
-- H2 in MODE=MySQL does not accept multiple ADD COLUMN / ADD CONSTRAINT clauses
-- in a single ALTER TABLE statement, so each clause is its own statement.

ALTER TABLE usuarios DROP COLUMN especialidad;

ALTER TABLE usuarios ADD COLUMN especialidad_id BINARY(16) NULL;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_especialidad
    FOREIGN KEY (especialidad_id) REFERENCES especialidades(id);
CREATE INDEX idx_usuarios_especialidad ON usuarios (especialidad_id);

ALTER TABLE paciente_contexto ADD COLUMN especialidad_id BINARY(16) NULL;
ALTER TABLE paciente_contexto ADD CONSTRAINT fk_paciente_contexto_especialidad
    FOREIGN KEY (especialidad_id) REFERENCES especialidades(id);
CREATE INDEX idx_paciente_contexto_especialidad ON paciente_contexto (especialidad_id);

ALTER TABLE articulos_cientificos ADD COLUMN especialidad_id BINARY(16) NULL;
ALTER TABLE articulos_cientificos ADD CONSTRAINT fk_articulos_especialidad
    FOREIGN KEY (especialidad_id) REFERENCES especialidades(id);
CREATE INDEX idx_articulos_especialidad ON articulos_cientificos (especialidad_id);
