# MedSync Backend — Guía para agentes

## Contexto del proyecto

MedSync es una plataforma de farmacovigilancia que:
- Analiza artículos científicos usando IA para extraer tags
- Cruza tags con el contexto clínico de pacientes (BD externa de hospital)
- Genera alertas sobre medicamentos obsoletos o nueva evidencia

## Stack

- **Framework**: Quarkus
- **Lenguaje**: Java 21
- **Base de datos**: MySQL (prod), H2 (dev)
- **ORM**: Hibernate ORM con Panache
- **Migraciones**: Flyway (los scripts viven en `src/main/resources/db/migration/`)
- **API**: REST + Jackson + OpenAPI (Swagger UI en `/q/swagger-ui`)
- **Validación**: Hibernate Validator

## Arquitectura: Clean Architecture con organización Capa > Feature

### 4 capas

```
src/main/java/itesm/medsync/
├── domain/             Reglas de negocio puras (cero dependencias externas)
├── application/        Implementación de casos de uso (orquestación)
├── infrastructure/     Implementaciones concretas + configuración
└── interfaces/         Puntos de entrada (REST)
```

### Estructura por feature

Cada feature tiene subcarpetas dentro de `domain/`:

```
domain/<feature>/
  model/           POJOs del negocio (sin anotaciones de framework)
  usecase/         Interfaces de casos de uso
  repository/      Interfaces de repositorios (*Repository) y gateways (*Gateway)
  exception/       Excepciones del negocio

application/<feature>/
  <UseCase>Service.java    Implementa la interfaz UseCase del dominio

infrastructure/persistence/<feature>/
  <Feature>Entity.java             @Entity JPA
  <Feature>RepositoryImpl.java     Implementa domain <Feature>Repository + PanacheRepository<Entity>
  <Feature>PersistenceMapper.java  Entity ↔ Domain

interfaces/rest/<feature>/
  <Feature>Resource.java           @Path JAX-RS
  <Action>Request.java             DTO de entrada
  <Feature>Response.java           DTO de salida
  <Feature>RestMapper.java         Domain ↔ DTO
```

## Reglas estrictas

### Dependency Rule (CRÍTICO)

```
domain/          ← NO importa nada externo (ni JPA, ni Quarkus, ni Jakarta)
application/     ← solo importa domain/
infrastructure/  ← importa domain/ + frameworks (JPA, Panache, HTTP clients)
interfaces/      ← importa domain/ + Jakarta REST
```

Si ves un import de `jakarta.persistence` en `domain/` o `application/` — es un bug.

### Nomenclatura

- **`*Repository`** → persistencia de datos propios de MedSync (CRUD)
- **`*Gateway`** → servicios o datos externos (BD del hospital, servicios de IA)
- Ambos viven en `domain/<feature>/repository/`

### Un service por use case

Cada interfaz `UseCase` tiene su propia clase `Service` que la implementa. NO un solo service que implemente múltiples use cases.

### DTOs obligatorios

Nunca exponer el domain model directamente via REST. Siempre:
- Input: `Request` DTO específico de la operación
- Output: `Response` DTO

### Doble mapeo

Cada feature tiene 2 mappers:
- `<Feature>PersistenceMapper` — Entity ↔ Domain (en `infrastructure/`)
- `<Feature>RestMapper` — Domain ↔ DTO (en `interfaces/`)

### RepositoryImpl merged con PanacheRepository

El `<Feature>RepositoryImpl` implementa AMBAS interfaces en la misma clase:

```java
@ApplicationScoped
@Transactional
public class PatientRepositoryImpl implements PatientRepository, PanacheRepository<PatientEntity> {

    @Override
    public Patient save(Patient patient) {
        PatientEntity entity = PatientPersistenceMapper.toEntity(patient);
        persist(entity);                        // método heredado de PanacheRepository
        return PatientPersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return findByIdOptional(id)             // heredado de PanacheRepository
                .map(PatientPersistenceMapper::toDomain);
    }
}
```

NO crear un archivo `<Feature>PanacheRepository.java` separado. Los métodos de Panache (`persist`, `findByIdOptional`, `listAll`, `list`, etc.) están disponibles directamente en el `RepositoryImpl` porque la clase misma implementa `PanacheRepository<Entity>`.

Las queries custom se escriben dentro del mismo `RepositoryImpl`:

```java
public List<PatientEntity> findActiveByMedico(UUID medicoId) {
    return list("medico.id = ?1 and activo = true", medicoId);
}
```

## Reglas de persistencia

### UUIDs en MySQL

Usar `BINARY(16)` para IDs. Configurar con:

```java
@Id
@Column(columnDefinition = "BINARY(16)")
private UUID id;
```

Hibernate hace la conversión automática — en Java siempre trabajas con `UUID`.

### Soft delete con @SQLRestriction

Entidades con soft delete (campo `activo: boolean`) usan:

```java
@Entity
@SQLRestriction("activo = true")
public class <Feature>Entity { ... }
```

Esto filtra automáticamente TODAS las queries. Para borrar, se hace `entity.setActivo(false)`.

### Auditoría automática

Campos `createdAt` y `updatedAt` se manejan automáticamente:

```java
@CreationTimestamp
@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(name = "updated_at")
private LocalDateTime updatedAt;
```

### SIEMPRE usar EntityGraph — nunca LAZY ni EAGER como estrategia

**Regla**: En `infrastructure/persistence/`, las relaciones JPA (`@ManyToOne`, `@OneToMany`, `@ManyToMany`) NUNCA deben depender de `FetchType.LAZY` ni `FetchType.EAGER` como estrategia de carga.

**Cómo aplicarla**:

1. Declarar las relaciones con `FetchType.LAZY` (es el default seguro)
2. NUNCA confiar en LAZY — cada query debe especificar qué cargar via EntityGraph
3. Definir `@NamedEntityGraph` en la Entity para cada combinación que se necesite
4. En queries, usar el graph como hint:

```java
@Entity
@NamedEntityGraph(
    name = "Patient.withMedico",
    attributeNodes = { @NamedAttributeNode("medico") }
)
public class PatientEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private UserEntity medico;
}
```

```java
EntityGraph<?> graph = entityManager.getEntityGraph("Patient.withMedico");
return entityManager
    .createQuery("SELECT p FROM PatientEntity p", PatientEntity.class)
    .setHint("jakarta.persistence.fetchgraph", graph)
    .getResultList();
```

**Por qué**:
- `EAGER` siempre carga aunque no se necesite (over-fetching + N+1)
- `LAZY` sin graph puede causar `LazyInitializationException`
- `EntityGraph` da control explícito por query, en una sola consulta con JOIN FETCH

**Nota**: Si una entity no tiene relaciones (solo columnas escalares), no necesita `@NamedEntityGraph`. La regla aplica cuando hay relaciones.

## Reglas de validación

Validación en **dos niveles**, con propósitos distintos:

### En el DTO (formato de entrada)
Anotaciones Jakarta Bean Validation:

```java
public class CreatePatientRequest {
    @NotBlank
    public String nombre;

    @NotNull @Past
    public LocalDate fechaNacimiento;
}
```

### En el Domain model (reglas de negocio)
Validaciones en el constructor del POJO:

```java
public Patient(UUID id, LocalDate fechaNacimiento, ...) {
    if (fechaNacimiento.isAfter(LocalDate.now())) {
        throw new InvalidPatientDataException("...");
    }
    // ...
}
```

## Reglas de migraciones

- Todas las migraciones viven en `src/main/resources/db/migration/`
- Nombre: `V<N>__<descripcion>.sql` (doble underscore)
- Nunca modificar un script ya commiteado — crear uno nuevo
- Usar SQL compatible con MySQL (prod) y H2 (dev) cuando sea posible
- Para features que referencian otros features aún no implementados: crear la columna sin FK constraint, agregar el FK en una migración posterior

Configuración en `application.properties`:

```properties
quarkus.flyway.migrate-at-start=true
quarkus.hibernate-orm.database.generation=validate
```

## Manejo de excepciones

Cada feature define sus excepciones de dominio en `domain/<feature>/exception/`. El `GlobalExceptionHandler` en `infrastructure/config/` las mapea a códigos HTTP:

| Tipo | HTTP |
|------|------|
| `<Resource>NotFoundException` | 404 |
| `Duplicate<X>Exception`, `<X>LimitExceededException` | 409 |
| `Invalid<X>DataException`, `ConstraintViolationException` | 400 |
| Cualquier otra | 500 |

## Features planeados

- `user` — Usuarios del sistema (médicos, admin, farmacéuticos)
- `patient` — Pacientes
- `medication` — Medicamentos
- `article` — Artículos científicos y sus tags
- `alert` — Alertas generadas por el cruce de datos

## Workflow al agregar un feature

1. **Crear el domain primero** (model, usecase, repository, exception)
2. **Implementar los services** en application
3. **Crear la Entity y migración Flyway** en infrastructure/persistence
4. **Implementar RepositoryImpl** con mappers
5. **Crear Resource + DTOs + RestMapper** en interfaces
6. **Actualizar GlobalExceptionHandler** si hay nuevas excepciones
7. **Verificar**: `./mvnw compile` y `./mvnw quarkus:dev`, probar con Swagger UI en `/q/swagger-ui`

## Qué NO hacer

- No exponer entities JPA ni domain models directamente por REST
- No importar frameworks en `domain/` ni `application/`
- No poner lógica de negocio en Resources ni en Services (va en el domain model)
- No usar `FetchType.EAGER` nunca
- No depender de LAZY sin EntityGraph
- No hacer borrado físico en features con soft delete
- No editar migraciones Flyway ya committeadas
- No crear un service que implemente múltiples use cases
