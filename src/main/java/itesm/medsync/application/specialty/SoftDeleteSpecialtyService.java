package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.exception.SpecialtyNotFoundException;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.specialty.usecase.SoftDeleteSpecialtyUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class SoftDeleteSpecialtyService implements SoftDeleteSpecialtyUseCase {

    private final SpecialtyRepository repository;

    @Inject
    public SoftDeleteSpecialtyService(SpecialtyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID id) {
        Specialty current = repository.findByUuid(id)
                .orElseThrow(() -> new SpecialtyNotFoundException(id));
        repository.save(current.softDelete());
    }
}
