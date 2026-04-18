# Feature: Patient

CRUD de pacientes con soft delete. No permite edición.

## Endpoints

| Método | Ruta | Propósito | Status codes |
|--------|------|-----------|--------------|
| `POST` | `/api/patients` | Crear paciente | 201 / 400 / 409 |
| `GET` | `/api/patients` | Listar pacientes activos | 200 |
| `GET` | `/api/patients/{id}` | Obtener un paciente por id | 200 / 404 |
| `DELETE` | `/api/patients/{id}` | Soft delete (marca `activo=false`) | 204 / 404 |

Swagger UI: `http://localhost:8080/q/swagger-ui`

## Modelo de dominio

`Patient` (inmutable) con los siguientes campos:

- `id: UUID`
- `expedienteExternoId: String` — identificador del expediente en la BD del hospital externo
- `nombre: String`
- `fechaNacimiento: LocalDate`
- `genero: String` (opcional)
- `medicoId: UUID` — referencia lógica al usuario médico (sin FK por ahora)
- `activo: boolean` — soft delete
- `createdAt`, `updatedAt: LocalDateTime` — auditoría automática por Hibernate

## Reglas de negocio

Validadas en el constructor de `Patient` (dominio puro):

- `nombre` no puede ser null ni blank
- `expedienteExternoId` no puede ser null ni blank
- `fechaNacimiento` no puede ser null ni futura
- Edad derivada ≤ 150 años
- `medicoId` no puede ser null
- `id` no puede ser null

Regla de unicidad (validada en `CreatePatientService`):

- `expedienteExternoId` es **único globalmente**, incluyendo pacientes soft-deleted.

## Validaciones en dos niveles

Cada regla se valida donde corresponde:

| Nivel | Dónde vive | Qué valida | Ejemplo |
|-------|------------|------------|---------|
| **Formato** | `CreatePatientRequest` (capa `interfaces/rest`) | Forma del payload HTTP | `@NotBlank`, `@Past`, `@NotNull`, `@Size` |
| **Negocio** | Constructor de `Patient` (capa `domain`) | Invariantes del modelo | Edad ≤ 150, fecha no futura |

Esta redundancia es intencional: el dominio no confía en quien lo invoque (ni REST, ni eventualmente gRPC, ni tests).

## DTOs (capa `interfaces/rest/patient/`)

- `CreatePatientRequest` — entrada del POST. Anotado con Jakarta Bean Validation.
- `PatientResponse` — salida de POST/GET/GET-by-id. Record inmutable.

Se usan DTOs explícitos en vez de exponer el dominio por dos razones:
1. **Seguridad**: si mañana agregas un campo sensible al `Patient` (ej: notas clínicas internas), no se filtra automáticamente por HTTP — solo sale lo que el `PatientRestMapper` explícitamente mapea.
2. **Desacoplamiento**: el dominio puede evolucionar sin romper el contrato JSON público.

El `DELETE` y el `GET /{id}` no tienen Request DTO porque el `id` viaja como `@PathParam` (URL).

## Arquitectura de capas

```
┌────────────────────────────────────────────────────────────────┐
│ interfaces/rest/patient/                                       │
│   PatientResource → CreatePatientRequest, PatientResponse      │
│                   → PatientRestMapper                          │
└──────────────────────────┬─────────────────────────────────────┘
                           │ recibe primitivos (String, UUID, LocalDate)
                           ▼
┌────────────────────────────────────────────────────────────────┐
│ application/patient/                                           │
│   CreatePatientService, GetPatientByIdService,                 │
│   ListActivePatientsService, DeletePatientService              │
│   (1 service por use case)                                     │
└──────────────────────────┬─────────────────────────────────────┘
                           │ implementa CreatePatientUseCase, ...
                           ▼
┌────────────────────────────────────────────────────────────────┐
│ domain/patient/                                                │
│   Patient (model), *UseCase (interfaces),                      │
│   PatientRepository (interface), 3 exceptions                  │
│   ← núcleo puro, sin dependencias de frameworks                │
└──────────────────────────▲─────────────────────────────────────┘
                           │ implementa PatientRepository
┌──────────────────────────┴─────────────────────────────────────┐
│ infrastructure/persistence/patient/                            │
│   PatientEntity (@Entity, @SQLRestriction("activo = true"))    │
│   PatientPersistenceMapper                                     │
│   PatientRepositoryImpl implements PatientRepository,          │
│                                   PanacheRepositoryBase<..,UUID>│
└────────────────────────────────────────────────────────────────┘
```

Dirección de dependencias: `interfaces` y `infrastructure` apuntan a `domain`. `domain` no conoce a nadie.

## Decisiones técnicas

### 1. `medicoId` como `UUID` puro (no `@ManyToOne User`)

El feature `user` todavía no existe. En vez de bloquear `patient`, guardamos `medicoId` como UUID escalar y **no creamos FK constraint** en la migración V1. Cuando se implemente `user`, una migración futura (ej: `V5__add_patients_medico_fk.sql`) agregará el `FOREIGN KEY`.

Consecuencia: el dominio `Patient` no tiene un objeto `User` dentro — solo un `UUID`. Así se respeta la regla "no referenciar features no implementados".

### 2. `findByUuid` en vez de `findById`

`PanacheRepositoryBase<PatientEntity, UUID>` expone un `findById(UUID) → PatientEntity`. Si la interfaz de dominio tuviera `findById(UUID) → Optional<Patient>`, Java rechaza la clase por **return type incompatibles** con misma firma.

Solución: en `PatientRepository` el método se llama `findByUuid(UUID)`. El precio es perder el nombre canónico; la ganancia es usar herencia directa (más limpia que composición con clase anidada).

### 3. Soft delete con `@SQLRestriction("activo = true")`

En `PatientEntity` se anota `@SQLRestriction("activo = true")`. Hibernate agrega `AND activo = true` a todas las queries HQL/JPQL. Resultado: `findAllActive()` y `findByUuid()` filtran automáticamente los soft-deleted.

El `delete()` físico nunca se llama — `DeletePatientService` invoca `patient.softDelete()` (devuelve nueva instancia con `activo=false`) y guarda.

### 4. Unicidad global de `expedienteExternoId` vía query nativa

Problema: `@SQLRestriction` también oculta soft-deleted del `COUNT` HQL. Si un paciente fue soft-deleted y se intenta crear otro con el mismo `expedienteExternoId`:

1. `existsByExpedienteExternoId` con HQL → `false` (el soft-deleted está filtrado)
2. Service construye el `Patient` y llama `save`
3. La `UNIQUE` constraint de SQL rechaza el INSERT → `500` crudo

Solución dual en `PatientRepositoryImpl`:
- `existsByExpedienteExternoId` usa **query nativa** (`SELECT COUNT(*) FROM patients WHERE ...`) que ignora `@SQLRestriction` y detecta también los soft-deleted.
- El `GlobalExceptionHandler` mapea `org.hibernate.exception.ConstraintViolationException` → **409**, como red de seguridad.

### 5. `save` bypassa `@SQLRestriction` intencionalmente

`PatientRepositoryImpl.save()` usa `findByIdOptional()` heredado de Panache (que internamente llama `session.find`, el cual bypassa `@SQLRestriction`). Esto permite **actualizar** una entity aunque esté soft-deleted — caso necesario durante el flujo de soft-delete (al momento del lookup la entity sigue activa en BD, pero si en el futuro se permitiera "resurrecter", este método lo soportaría sin código extra).

En contraste, `findByUuid` usa `find("id", id)` HQL que **sí** respeta `@SQLRestriction` — es la vista "pública" que no expone soft-deleted.

### 6. Hibernate en modo `validate`

`application.properties` tiene:
```
quarkus.hibernate-orm.database.generation=validate
quarkus.flyway.migrate-at-start=true
```

La migración Flyway `V1__create_patients_table.sql` es la única fuente de verdad del schema. Hibernate solo **valida** que los `@Entity` coincidan — nunca crea ni modifica tablas en runtime. Evita drift entre `@Entity` y schema real.

### 7. H2 (dev/test) en `MODE=MySQL`

La URL JDBC de H2 usa `MODE=MySQL` para que los tipos se comporten como en producción: `BINARY(16)`, `BOOLEAN`, `TIMESTAMP DEFAULT CURRENT_TIMESTAMP` funcionan igual. Una sola migración SQL corre en ambas BDs sin branches.

### 8. Validación doble (DTO + Domain)

El `CreatePatientRequest` valida formato con Jakarta Bean Validation. El constructor de `Patient` valida reglas de negocio. Ambas validan "fecha no futura" — intencional: si alguien llama al `CreatePatientService` desde un test o un futuro consumidor gRPC sin pasar por REST, el dominio sigue protegido.

## Tests

Cobertura en capas:

| Archivo | Tipo | Cubre |
|---------|------|-------|
| `PatientTest` | Unit (JUnit 5 puro) | Constructor, validaciones, `softDelete`, `equals/hashCode` |
| `*ServiceTest` | Unit (Mockito) | Lógica de orquestación, excepciones de negocio |
| `PatientRepositoryImplTest` | Integración (`@QuarkusTest` + `@TestTransaction`) | `@SQLRestriction`, query nativa, timestamps, save/update |
| `PatientResourceIT` | Integración REST (`@QuarkusTest` + RestAssured) | Todos los endpoints + status codes + flujo end-to-end |

Ejecutar: `./mvnw test`

## Migración

`src/main/resources/db/migration/V1__create_patients_table.sql`

```sql
CREATE TABLE patients (
    id BINARY(16) NOT NULL,
    expediente_externo_id VARCHAR(100) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    genero VARCHAR(20) NULL,
    medico_id BINARY(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT uq_patients_expediente UNIQUE (expediente_externo_id)
);

CREATE INDEX idx_patients_medico ON patients (medico_id);
CREATE INDEX idx_patients_activo ON patients (activo);
```

- `BINARY(16)` para UUIDs (MySQL + H2 `MODE=MySQL`)
- Sin FK sobre `medico_id` (feature `user` aún no existe)
- `UNIQUE` global sobre `expediente_externo_id` (incluye soft-deleted)
- Índices en `medico_id` (queries futuras por médico) y `activo` (filtro del `@SQLRestriction`)

## Fuera de alcance (intencional)

- Edición (`PUT`/`PATCH`)
- Restauración de pacientes soft-deleted
- Paginación o filtros en el listado
- FK constraint sobre `medico_id`
- Autenticación/autorización

## Siguientes features relacionados

- `user` → agregará entity de usuarios y, en una migración futura, el FK `patients.medico_id → users.id`
- `alert` → generará alertas cruzando pacientes activos con artículos científicos
