package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.exception.SpecialtyNotFoundException;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.specialty.usecase.GetSpecialtyByIdUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GetSpecialtyByIdService implements GetSpecialtyByIdUseCase {

    private final SpecialtyRepository repository;

    @Inject
    public GetSpecialtyByIdService(SpecialtyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Specialty execute(UUID id) {
        return repository.findByUuid(id)
                .orElseThrow(() -> new SpecialtyNotFoundException(id));
    }
}
