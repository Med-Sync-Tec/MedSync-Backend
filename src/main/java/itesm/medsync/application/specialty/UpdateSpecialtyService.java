package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.exception.DuplicateSpecialtyException;
import itesm.medsync.domain.specialty.exception.SpecialtyNotFoundException;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.specialty.usecase.UpdateSpecialtyUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service that replaces the editable fields of an existing specialty.
 *
 * Uniqueness checks skip the current row's own values, so a no-op rename does not
 * trip the duplicate guard against itself.
 */
@ApplicationScoped
public class UpdateSpecialtyService implements UpdateSpecialtyUseCase {

    private final SpecialtyRepository repository;

    @Inject
    public UpdateSpecialtyService(SpecialtyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Specialty execute(UUID id, String nombre, String slug, String descripcion) {
        Specialty current = repository.findByUuid(id)
                .orElseThrow(() -> new SpecialtyNotFoundException(id));

        // Skip the duplicate guard when the value is unchanged: otherwise the
        // current row would collide with itself.
        if (nombre != null && !Objects.equals(nombre, current.getNombre())
                && repository.existsByNombreActive(nombre)) {
            throw new DuplicateSpecialtyException("nombre", nombre);
        }
        if (slug != null && !Objects.equals(slug, current.getSlug())
                && repository.existsBySlugAcrossAllRows(slug)) {
            throw new DuplicateSpecialtyException("slug", slug);
        }

        Specialty updated = current
                .withNombre(nombre)
                .withSlug(slug)
                .withDescripcion(descripcion);
        return repository.save(updated);
    }
}
