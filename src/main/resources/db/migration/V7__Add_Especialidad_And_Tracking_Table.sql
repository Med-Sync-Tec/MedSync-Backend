ALTER TABLE usuarios ADD COLUMN especialidad VARCHAR(100);

CREATE TABLE usuario_articulos_leidos (
    usuario_id BINARY(16) NOT NULL,
    articulo_id BINARY(16) NOT NULL,
    leido_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, articulo_id),
    CONSTRAINT fk_ual_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_ual_articulo FOREIGN KEY (articulo_id) REFERENCES articulos_cientificos(id) ON DELETE CASCADE
);
