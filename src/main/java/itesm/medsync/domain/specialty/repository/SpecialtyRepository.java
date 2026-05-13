package itesm.medsync.domain.specialty.repository;

import itesm.medsync.domain.specialty.model.Specialty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecialtyRepository {

    Specialty save(Specialty specialty);

    Optional<Specialty> findByUuid(UUID id);

    Optional<Specialty> findBySlug(String slug);

    List<Specialty> findAllActive();

    boolean existsByNombreActive(String nombre);

    boolean existsBySlugAcrossAllRows(String slug);
}
