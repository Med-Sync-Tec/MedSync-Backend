CREATE TABLE articulos_cientificos (
    id BINARY(16) NOT NULL,
    titulo VARCHAR(1000) NOT NULL, -- NOSONAR
    autores TEXT NULL,
    revista VARCHAR(500) NULL, -- NOSONAR
    anio_pub INT NULL,
    mes_pub VARCHAR(20) NULL, -- NOSONAR
    doi VARCHAR(200) NULL, -- NOSONAR
    abstract_text TEXT NULL,
    keywords TEXT NULL,
    tipo_publicacion VARCHAR(100) NULL, -- NOSONAR
    url VARCHAR(500) NULL, -- NOSONAR
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_articulos_cientificos PRIMARY KEY (id),
    CONSTRAINT uq_articulos_doi UNIQUE (doi)
);

CREATE INDEX idx_articulos_anio ON articulos_cientificos (anio_pub);

CREATE TABLE articulo_tags (
    id BINARY(16) NOT NULL,
    articulo_id BINARY(16) NOT NULL,
    tipo VARCHAR(30) NOT NULL, -- NOSONAR
    valor VARCHAR(500) NOT NULL, -- NOSONAR
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_articulo_tags PRIMARY KEY (id),
    CONSTRAINT fk_articulo_tags_articulo
        FOREIGN KEY (articulo_id) REFERENCES articulos_cientificos(id)
);

CREATE INDEX idx_articulo_tags_articulo ON articulo_tags (articulo_id);
CREATE INDEX idx_articulo_tags_lookup ON articulo_tags (articulo_id, tipo, valor);
CREATE INDEX idx_articulo_tags_match ON articulo_tags (tipo, valor);
