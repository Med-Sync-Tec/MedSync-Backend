package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.exception.DuplicateSpecialtyException;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.specialty.usecase.CreateSpecialtyUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Application service that creates a specialty.
 *
 * Performs uniqueness checks before delegating to the domain factory, so the
 * caller gets a meaningful {@link DuplicateSpecialtyException} instead of a
 * raw SQL constraint violation from the persistence layer.
 */
@ApplicationScoped
public class CreateSpecialtyService implements CreateSpecialtyUseCase {

    private final SpecialtyRepository repository;

    @Inject
    public CreateSpecialtyService(SpecialtyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Specialty execute(String nombre, String slug, String descripcion) {
        // Nombre uniqueness is enforced only among active rows: a soft-deleted
        // specialty's name can be reused if the operator wants to "rename" by
        // recreating, which is the COO's typical workflow.
        if (nombre != null && repository.existsByNombreActive(nombre)) {
            throw new DuplicateSpecialtyException("nombre", nombre);
        }
        // Slugs are globally unique across history (active + soft-deleted), so
        // a recycled slug cannot resurrect old URL paths or vocabulary files
        // under a new specialty identity.
        if (slug != null && repository.existsBySlugAcrossAllRows(slug)) {
            throw new DuplicateSpecialtyException("slug", slug);
        }
        Specialty specialty = Specialty.create(nombre, slug, descripcion);
        return repository.save(specialty);
    }
}
