package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.specialty.usecase.ListActiveSpecialtiesUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ListActiveSpecialtiesService implements ListActiveSpecialtiesUseCase {

    private final SpecialtyRepository repository;

    @Inject
    public ListActiveSpecialtiesService(SpecialtyRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Specialty> execute() {
        return repository.findAllActive();
    }
}
