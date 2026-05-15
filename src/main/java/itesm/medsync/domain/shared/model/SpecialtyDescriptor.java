package itesm.medsync.domain.shared.model;

import java.util.UUID;

/**
 * Minimal specialty projection shipped to the LLM during classification.
 *
 * Carries only the fields the model needs to disambiguate ({@code nombre},
 * {@code slug}, {@code descripcion}). Lives in {@code domain/shared} because
 * both AI features (article and consulta analysis) consume it.
 */
public record SpecialtyDescriptor(UUID id, String nombre, String slug, String descripcion) {
}
