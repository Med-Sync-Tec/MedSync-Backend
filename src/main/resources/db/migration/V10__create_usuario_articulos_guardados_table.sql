CREATE TABLE usuario_articulos_guardados (
    usuario_id BINARY(16) NOT NULL,
    articulo_id BINARY(16) NOT NULL,
    guardado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, articulo_id),
    CONSTRAINT fk_uag_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_uag_articulo FOREIGN KEY (articulo_id) REFERENCES articulos_cientificos(id) ON DELETE CASCADE
);
