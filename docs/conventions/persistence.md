# Persistence Conventions

## UUIDs

All primary keys are UUIDs stored as `BINARY(16)` in MySQL/H2.

```java
@Id
@Column(columnDefinition = "BINARY(16)")
private UUID id;
```

Hibernate handles the conversion — in Java we always work with `UUID`, never with byte arrays. In SQL migrations:

```sql
id BINARY(16) NOT NULL,
CONSTRAINT pk_<table> PRIMARY KEY (id)
```

## Soft delete

Entities with a soft-delete flag (boolean `activo` column) use `@SQLRestriction`:

```java
@Entity
@SQLRestriction("activo = true")
public class PatientEntity { ... }
```

Hibernate appends `AND activo = true` to every JPQL/HQL query against this entity. To delete, call `setActivo(false)` and `persist` — never `entity.delete()` for soft-delete features.

### Gotcha: `@SQLRestriction` also hides soft-deleted rows from `COUNT` and uniqueness checks

If you need to enforce uniqueness across all rows including soft-deleted ones, use a **native query**:

```java
public boolean existsByExpedienteExternoId(String value) {
    return getEntityManager()
        .createNativeQuery("SELECT COUNT(*) FROM patients WHERE expediente_externo_id = ?")
        .setParameter(1, value)
        .getSingleResult()
        .toString()
        .equals("0") ? false : true;
}
```

`@SQLRestriction` is bypassed by native SQL. See [specs/patient/1-05-2026/design.md](../specs/patient/1-05-2026/design.md) for the full pattern.

## Auditing (`createdAt`, `updatedAt`)

Use Hibernate annotations — never set these manually:

```java
@CreationTimestamp
@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(name = "updated_at")
private LocalDateTime updatedAt;
```

## Fetch strategy: always use EntityGraph, never rely on LAZY or EAGER

**Rule**: every relation (`@ManyToOne`, `@OneToMany`, `@ManyToMany`) is declared `FetchType.LAZY`. Every query that needs the relation specifies it via `@NamedEntityGraph`.

**Why**:

- `EAGER` over-fetches: every query loads the relation even when not needed.
- `LAZY` without a graph throws `LazyInitializationException` outside the persistence context.
- `EntityGraph` gives explicit, per-query control. Hibernate generates a single SQL with `LEFT JOIN FETCH`.

### How to apply

1. Declare the relation `LAZY`:

   ```java
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "medico_id", nullable = false)
   private UserEntity medico;
   ```

2. Define one `@NamedEntityGraph` per fetch combination you need:

   ```java
   @Entity
   @NamedEntityGraph(
       name = "Patient.withMedico",
       attributeNodes = { @NamedAttributeNode("medico") }
   )
   public class PatientEntity { ... }
   ```

3. Use the graph as a query hint:

   ```java
   EntityGraph<?> graph = getEntityManager().getEntityGraph("Patient.withMedico");
   return getEntityManager()
       .createQuery("SELECT p FROM PatientEntity p WHERE p.id = :id", PatientEntity.class)
       .setParameter("id", id)
       .setHint("jakarta.persistence.fetchgraph", graph)
       .getResultStream()
       .findFirst();
   ```

If an entity has no relations (only scalar columns), no graph is needed.

## `RepositoryImpl` implements both the domain port and Panache

```java
@ApplicationScoped
@Transactional
public class PatientRepositoryImpl
    implements PatientRepository, PanacheRepository<PatientEntity> {

    @Override
    public Patient save(Patient patient) {
        PatientEntity entity = PatientPersistenceMapper.toEntity(patient);
        persist(entity);                                     // Panache method
        return PatientPersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<Patient> findByUuid(UUID id) {
        return findByIdOptional(id)                          // Panache method
            .map(PatientPersistenceMapper::toDomain);
    }
}
```

Do **not** create a separate `PatientPanacheRepository.java`. Composition adds boilerplate without benefit.

Custom queries live in the same class:

```java
public List<PatientEntity> findActiveByMedico(UUID medicoId) {
    return list("medico.id = ?1 and activo = true", medicoId);
}
```

## Persistence mapper

One static class per feature: `<Feature>PersistenceMapper` with:

- `toEntity(<Feature> domain) → <Feature>Entity`
- `toDomain(<Feature>Entity entity) → <Feature>`
- For collections: `toDomainList(List<...>)` or use stream mapping inline.

No CDI bean — mappers are stateless utilities.

## Two datasources

The default persistence unit owns `infrastructure.persistence.*`. The `hospital` persistence unit owns `infrastructure.hospital.*`. They must not share an `@Entity`.

Configuration enforces this:

```properties
quarkus.hibernate-orm.packages=itesm.medsync.infrastructure.persistence
quarkus.hibernate-orm.hospital.packages=itesm.medsync.infrastructure.hospital
```

`HospitalGatewayImpl` uses raw `EntityManager` (`@PersistenceContext(unitName="hospital")`) because it handles two entities that don't share a `PanacheRepository`. See [ADR 0003](../architecture/decisions/0003-two-datasources-medsync-hospital.md).

## Transactions

- Repositories are annotated `@Transactional` at class level.
- Application services are annotated `@Transactional` when they perform multiple repository writes.
- Cross-datasource transactions are **not atomic** — we do not use XA. Document this in service-level Javadoc when relevant.
