package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.exception.DuplicateSpecialtyException;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.specialty.usecase.CreateSpecialtyUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CreateSpecialtyService implements CreateSpecialtyUseCase {

    private final SpecialtyRepository repository;

    @Inject
    public CreateSpecialtyService(SpecialtyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Specialty execute(String nombre, String slug, String descripcion) {
        if (nombre != null && repository.existsByNombreActive(nombre)) {
            throw new DuplicateSpecialtyException("nombre", nombre);
        }
        if (slug != null && repository.existsBySlugAcrossAllRows(slug)) {
            throw new DuplicateSpecialtyException("slug", slug);
        }
        Specialty specialty = Specialty.create(nombre, slug, descripcion);
        return repository.save(specialty);
    }
}
