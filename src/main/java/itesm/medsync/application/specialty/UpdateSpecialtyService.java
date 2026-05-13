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
