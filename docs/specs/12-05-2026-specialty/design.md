# Feature: Specialty — Design

## Domain model

### `Specialty` (immutable, `domain/specialty/model/Specialty.java`)

| Field         | Type            | Required | Notes                                                                 |
|---------------|-----------------|----------|-----------------------------------------------------------------------|
| id            | `UUID`          | yes      | server-generated; seeded entries use deterministic UUIDs              |
| nombre        | `String`        | yes      | 1..100 chars; unique among **active** rows                            |
| slug          | `String`        | yes      | 1..60 chars; kebab-case (`^[a-z0-9]+(-[a-z0-9]+)*$`); globally unique (active **and** soft-deleted) — backs the vocabulary filename in the `medical-vocabulary` feature |
| descripcion   | `String`        | no       | up to 500 chars                                                       |
| activo        | `boolean`       | yes      | soft-delete flag (`true` by default)                                  |
| createdAt     | `LocalDateTime` | auto     | Hibernate                                                             |
| updatedAt     | `LocalDateTime` | auto     | Hibernate                                                             |

### Invariants (validated in constructor)

- `id` not null.
- `nombre`, `slug` not null nor blank.
- `slug` matches the kebab-case regex `^[a-z0-9]+(-[a-z0-9]+)*$`.
- `nombre` ≤ 100 chars; `slug` ≤ 60 chars; `descripcion` ≤ 500 chars when present.

### Behavior

- `Specialty.create(nombre, slug, descripcion)` — generates the UUID and initializes `activo = true`.
- `Specialty.softDelete()` — returns a new instance with `activo = false`.
- `Specialty.withNombre(...)`, `Specialty.withSlug(...)`, `Specialty.withDescripcion(...)` — copy-on-write updates used by the admin update flow.

The domain model has **no behaviors that reach out** to vocabulary, users, or articles. Cross-feature concerns are handled in services.

## Output port

```java
public interface SpecialtyRepository {
    Specialty save(Specialty specialty);
    Optional<Specialty> findByUuid(UUID id);
    Optional<Specialty> findBySlug(String slug);
    List<Specialty> findAllActive();                       // ordered alphabetically by nombre
    boolean existsByNombreActive(String nombre);           // active rows only
    boolean existsBySlugAcrossAllRows(String slug);        // bypasses @SQLRestriction via native query
}
```

`existsBySlugAcrossAllRows` mirrors the `existsByExpedienteExternoId` pattern from `patient` — slugs must be unique across both active and soft-deleted rows because they back filesystem resources (see [conventions/persistence.md](../../conventions/persistence.md#gotcha-sqlrestriction-also-hides-soft-deleted-rows-from-count-and-uniqueness-checks)).

`SpecialtyRepository` is a **`*Repository`** (own data, full CRUD), per [ADR 0002](../../architecture/decisions/0002-gateway-vs-repository.md).

## Use cases

| Use case interface             | Service                          | Trigger / endpoint                                  |
|--------------------------------|----------------------------------|-----------------------------------------------------|
| `ListActiveSpecialtiesUseCase` | `ListActiveSpecialtiesService`   | `GET /api/especialidades`                           |
| `GetSpecialtyByIdUseCase`      | `GetSpecialtyByIdService`        | `GET /api/especialidades/{id}` + internal validators |
| `CreateSpecialtyUseCase`       | `CreateSpecialtyService`         | `POST /api/admin/especialidades`                    |
| `UpdateSpecialtyUseCase`       | `UpdateSpecialtyService`         | `PUT /api/admin/especialidades/{id}`                |
| `SoftDeleteSpecialtyUseCase`   | `SoftDeleteSpecialtyService`     | `DELETE /api/admin/especialidades/{id}`             |

`GetSpecialtyByIdService` is reused by other features as the canonical existence check — `user` admin-create calls it to validate `especialidadId`, and `patient-context` reads the caller's specialty through it before saving an entry.

## REST endpoints

| Method | Path                                  | Request DTO              | Response DTO               | Status codes              |
|--------|---------------------------------------|--------------------------|----------------------------|---------------------------|
| GET    | `/api/especialidades`                 | —                        | `List<SpecialtyResponse>`  | 200 / 401                 |
| GET    | `/api/especialidades/{id}`            | —                        | `SpecialtyResponse`        | 200 / 401 / 404           |
| POST   | `/api/admin/especialidades`           | `CreateSpecialtyRequest` | `SpecialtyResponse`        | 201 / 400 / 401 / 403 / 409 |
| PUT    | `/api/admin/especialidades/{id}`      | `UpdateSpecialtyRequest` | `SpecialtyResponse`        | 200 / 400 / 401 / 403 / 404 / 409 |
| DELETE | `/api/admin/especialidades/{id}`      | —                        | —                          | 204 / 401 / 403 / 404     |

Swagger tags: `Specialties` (public listing) and `Admin` (CRUD). All endpoints require `AuthenticatedUserContext`; admin endpoints additionally check `caller.roleName().equalsIgnoreCase("COO")` — same manual-check pattern as `CreateAdminUserService` (see [specs/1-05-2026-user/design.md](../1-05-2026-user/design.md#3-manual-role-checks-not-rolesallowed)).

### DTOs (`interfaces/rest/specialty/`)

- `CreateSpecialtyRequest`
  - `nombre` — `@NotBlank @Size(max=100)`
  - `slug` — `@NotBlank @Size(max=60) @Pattern("^[a-z0-9]+(-[a-z0-9]+)*$")`
  - `descripcion` — `@Size(max=500)` (optional)
- `UpdateSpecialtyRequest` — same shape as create. All three fields required (`PUT` semantics).
- `SpecialtyResponse` — `record (UUID id, String nombre, String slug, String descripcion, boolean activo, LocalDateTime createdAt, LocalDateTime updatedAt)`.

## Persistence

### `SpecialtyEntity` (table `especialidades`)

| Column        | Type            | Constraints                                                              |
|---------------|-----------------|--------------------------------------------------------------------------|
| id            | `BINARY(16)`    | PK                                                                       |
| nombre        | `VARCHAR(100)`  | NOT NULL                                                                 |
| slug          | `VARCHAR(60)`   | NOT NULL, UNIQUE (global)                                                |
| descripcion   | `VARCHAR(500)`  | NULL                                                                     |
| activo        | `BOOLEAN`       | NOT NULL DEFAULT TRUE                                                    |
| created_at    | `TIMESTAMP`     | NOT NULL DEFAULT CURRENT_TIMESTAMP                                       |
| updated_at    | `TIMESTAMP`     | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP           |

Indexes:
- `uq_especialidades_slug` — `UNIQUE (slug)` (covers slug existence check)
- `idx_especialidades_activo` — `(activo)` (covers the listing endpoint's filter)

Uniqueness on `nombre` is **not** enforced at the DB level — it is checked at the service layer against active rows only (`SpecialtyRepository.existsByNombreActive`). This intentional asymmetry mirrors the soft-delete behavior: a deleted specialty's `nombre` is freed for reuse, but its `slug` is locked forever (because the JSON vocabulary file on disk may persist).

Annotated with `@SQLRestriction("activo = true")` so soft-deleted rows are filtered out of all JPQL queries by default — same pattern as `PatientEntity`. The `existsBySlugAcrossAllRows` method uses a native query to bypass the restriction (see [conventions/persistence.md](../../conventions/persistence.md#gotcha-sqlrestriction-also-hides-soft-deleted-rows-from-count-and-uniqueness-checks)).

The entity has no `@ManyToOne` relations — `Specialty` is a leaf reference target, not a parent. Owning entities (`UserEntity`, `PacienteContextoEntity`, `ArticleEntity`) point **to** it. Therefore no `@NamedEntityGraph` is needed on `SpecialtyEntity` itself (per [conventions/persistence.md](../../conventions/persistence.md#fetch-strategy-always-use-entitygraph-never-rely-on-lazy-or-eager)).

### Migrations

Three migrations land in this snapshot, in order:

#### `V9__create_especialidades_table.sql`

```sql
CREATE TABLE especialidades (
    id BINARY(16) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    descripcion VARCHAR(500) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_especialidades PRIMARY KEY (id),
    CONSTRAINT uq_especialidades_slug UNIQUE (slug)
);

CREATE INDEX idx_especialidades_activo ON especialidades (activo);
```

#### `V10__seed_especialidades.sql`

Idempotent seed with deterministic UUIDs (prefix `0x000000000000200080000000000000XX`, mirroring the `roles` seed convention from V3 with `2000` instead of `1000` as the namespace nibble — keeps role and specialty seed namespaces disjoint and readable in `mysql -e 'SELECT HEX(id) FROM …'`):

```sql
INSERT INTO especialidades (id, nombre, slug, descripcion) VALUES
    (UNHEX('00000000000020008000000000000001'), 'Cardiología',       'cardiologia',         'Diseases of the heart and vascular system'),
    (UNHEX('00000000000020008000000000000002'), 'Endocrinología',    'endocrinologia',      'Hormonal and metabolic disorders'),
    (UNHEX('00000000000020008000000000000003'), 'Neurología',        'neurologia',          'Disorders of the nervous system'),
    (UNHEX('00000000000020008000000000000004'), 'Oncología',         'oncologia',           'Cancer diagnosis and treatment'),
    (UNHEX('00000000000020008000000000000005'), 'Pediatría',         'pediatria',           'Medical care of infants, children, and adolescents'),
    (UNHEX('00000000000020008000000000000006'), 'Gastroenterología', 'gastroenterologia',   'Digestive system disorders'),
    (UNHEX('00000000000020008000000000000007'), 'Neumología',        'neumologia',          'Respiratory system diseases'),
    (UNHEX('00000000000020008000000000000008'), 'Dermatología',      'dermatologia',        'Skin, hair, and nail conditions'),
    (UNHEX('00000000000020008000000000000009'), 'Ginecología',       'ginecologia',         'Female reproductive system'),
    (UNHEX('0000000000002000800000000000000A'), 'Urología',          'urologia',            'Urinary tract and male reproductive system'),
    (UNHEX('0000000000002000800000000000000B'), 'Psiquiatría',       'psiquiatria',         'Mental, emotional, and behavioral disorders'),
    (UNHEX('0000000000002000800000000000000C'), 'Reumatología',      'reumatologia',        'Joint, muscle, and autoimmune diseases'),
    (UNHEX('0000000000002000800000000000000D'), 'Oftalmología',      'oftalmologia',        'Eye and vision disorders'),
    (UNHEX('0000000000002000800000000000000E'), 'Hematología',       'hematologia',         'Blood and blood-forming organ disorders'),
    (UNHEX('0000000000002000800000000000000F'), 'Medicina Interna',  'medicina-interna',    'Adult internal medicine'),
    (UNHEX('00000000000020008000000000000010'), 'Infectología',      'infectologia',        'Infectious diseases')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);
```

16 rows. Each one will have a corresponding `src/main/resources/vocabulary/<slug>.json` once the `medical-vocabulary` feature ships.

#### `V11__add_especialidad_id_to_existing_tables.sql`

Adds a nullable `especialidad_id BINARY(16)` column to `usuarios`, `paciente_contexto`, and `articulos_cientificos`, with FK constraints back to `especialidades(id)`. The column is **nullable** so existing rows remain valid without a backfill — historical data simply has no specialty.

```sql
ALTER TABLE usuarios
    ADD COLUMN especialidad_id BINARY(16) NULL,
    ADD CONSTRAINT fk_usuarios_especialidad
        FOREIGN KEY (especialidad_id) REFERENCES especialidades(id);
CREATE INDEX idx_usuarios_especialidad ON usuarios (especialidad_id);

ALTER TABLE paciente_contexto
    ADD COLUMN especialidad_id BINARY(16) NULL,
    ADD CONSTRAINT fk_paciente_contexto_especialidad
        FOREIGN KEY (especialidad_id) REFERENCES especialidades(id);
CREATE INDEX idx_paciente_contexto_especialidad ON paciente_contexto (especialidad_id);

ALTER TABLE articulos_cientificos
    ADD COLUMN especialidad_id BINARY(16) NULL,
    ADD CONSTRAINT fk_articulos_especialidad
        FOREIGN KEY (especialidad_id) REFERENCES especialidades(id);
CREATE INDEX idx_articulos_especialidad ON articulos_cientificos (especialidad_id);
```

The `articulos_cientificos.especialidad_id` index is the key to the upcoming matching-query filter (`article-ai-analysis`).

## Validation

| Level     | Check                                                                         | Location                          |
|-----------|-------------------------------------------------------------------------------|-----------------------------------|
| Format    | `@NotBlank @Size(max=100)` on `nombre`                                        | `CreateSpecialtyRequest`, `UpdateSpecialtyRequest` |
| Format    | `@NotBlank @Size(max=60) @Pattern("^[a-z0-9]+(-[a-z0-9]+)*$")` on `slug`      | `CreateSpecialtyRequest`, `UpdateSpecialtyRequest` |
| Format    | `@Size(max=500)` on `descripcion`                                             | both requests                     |
| Business  | `nombre`, `slug` not blank; `slug` matches kebab-case regex                   | `Specialty` constructor           |
| App-level | `nombre` not already used by another active specialty                         | `CreateSpecialtyService`, `UpdateSpecialtyService` |
| App-level | `slug` not already used by **any** row (active or soft-deleted)               | `CreateSpecialtyService`, `UpdateSpecialtyService` |
| App-level | Specialty exists for the given id (cross-feature: user-admin, patient-context) | `GetSpecialtyByIdService`        |

## Exceptions

| Domain exception                  | HTTP | Notes                                                            |
|-----------------------------------|------|------------------------------------------------------------------|
| `SpecialtyNotFoundException`      | 404  | Thrown by lookup and update/delete services.                     |
| `DuplicateSpecialtyException`     | 409  | Thrown when `nombre` or `slug` conflicts on create / update.     |
| `InvalidSpecialtyDataException`   | 400  | Constructor invariants violated.                                 |
| `RoleMismatchException` (reused)  | 403  | When a non-COO calls an admin endpoint.                          |

All four are wired into `GlobalExceptionHandler` (the first three are new; the fourth already exists for the `user` admin endpoint).

## Sequence flows

### Create specialty (COO)

```
Client          SpecialtyResource    CreateSpecialtyService    SpecialtyRepository      DB
  │  POST /api/admin/especialidades                  │                  │                  │
  │ ──────────────────────►                          │                  │                  │
  │                      assertCallerIsCoo()         │                  │                  │
  │                      execute(...) ────────────────►                 │                  │
  │                                       existsByNombreActive(nombre)  │                  │
  │                                       ──────────────────────────────►                  │
  │                                                          SELECT … (active)             │
  │                                       ◄────── false ───────────────  │                 │
  │                                       existsBySlugAcrossAllRows(slug)│                 │
  │                                       ──────────────────────────────►                  │
  │                                                  native SELECT (no @SQLRestriction)    │
  │                                       ◄────── false ───────────────  │                 │
  │                                       Specialty s = Specialty.create(...)              │
  │                                       save(s) ─────────────────────►                   │
  │                                                          INSERT                        │
  │                                       ◄──── Specialty ────────────  │                  │
  │                      ◄──── Specialty ────────────────────────────── │                  │
  │ ◄── 201 + Location ──│                                              │                  │
```

### Cross-feature: doctor adds a `PacienteContexto`

```
Client            PacienteContextoResource    AddPacienteContextoService    AuthCtx       SpecRepo     PCRepo      DB
  │  POST /api/patients/{p}/contextos              │                          │              │            │          │
  │ ──────────────────────────►                     │                          │              │            │          │
  │                       caller ← AuthenticatedUserContext.get()              │              │            │          │
  │                       execute(patientId, tipo, valor, caller.user) ───────►              │            │          │
  │                                            patient ← patientRepo.findByUuid(patientId)   │            │          │
  │                                            specialtyId ← caller.user.especialidadId      │            │          │
  │                                              (may be null for legacy users)              │            │          │
  │                                            PacienteContexto pc = new …(pacienteId, tipo, valor, specialtyId)     │
  │                                            pcRepo.save(pc) ─────────────────────────────────────────►  INSERT    │
  │ ◄── 201 + body ───────│                                                                                          │
```

The auth context exposes the caller's `User`, which after this feature includes `especialidadId`. The patient-context service reads it directly — no extra DB round-trip to `SpecialtyRepository`. If the caller's `especialidadId` is null (a legacy DOCTOR seeded before this feature), the contexto is persisted with `especialidad_id = NULL` — matching queries downstream will simply not match it for AI-driven flows, which is the safe failure mode.

### Cross-feature: COO creates a user

```
Client            AdminUserResource    CreateAdminUserService    SpecialtyRepo   FirebaseGw   UserRepo    DB
  │  POST /api/admin/users {especialidadId, …}  │                       │             │           │         │
  │ ──────────────────────►                      │                       │             │           │         │
  │                      assertCallerIsCoo()     │                       │             │           │         │
  │                      execute(…) ─────────────►                       │             │           │         │
  │                                   if especialidadId != null:         │             │           │         │
  │                                     getSpecialtyByIdService.execute(especialidadId)│           │         │
  │                                     ───────────────────────────────► │             │           │         │
  │                                                    SELECT (active)   │             │           │         │
  │                                     ◄── Specialty / throw NotFound ──│             │           │         │
  │                                   createFirebaseUser(…) ────────────────────────► │           │         │
  │                                   user ← new User(…, especialidadId, …)            │           │         │
  │                                   userRepo.save(user) ───────────────────────────────────────► INSERT    │
  │ ◄── 201 + body ─────│                                                                                     │
```

`especialidadId` in the request is **optional**: COO may omit it (the user is created without a specialty, matching the legacy behavior for backwards compatibility); if present, it must reference an active specialty or the call is rejected with 404 before the Firebase user is created.

## Cross-feature impact

These three changes ship together with this spec's migrations. They modify the schema of existing features without re-snapshotting them:

| Existing feature   | Affected artifact                                  | Change                                                                                                                                | Migration that lands it |
|--------------------|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|-------------------------|
| `user`             | `usuarios` table                                   | Adds nullable `especialidad_id BINARY(16) NULL` with FK to `especialidades(id)` and an index.                                         | `V11`                   |
| `user`             | `UserEntity`                                       | Adds `@ManyToOne(LAZY)` to `SpecialtyEntity` on `especialidad_id`; new `@NamedEntityGraph("User.withRoleAndSpecialty")`.              | (no migration; code only) |
| `user`             | `User` domain model                                | Adds nullable `UUID especialidadId` field; constructor invariants unchanged for null case.                                            | (no migration; code only) |
| `user`             | `CreateUserRequest` DTO                            | Adds optional `UUID especialidadId` (nullable, no `@NotNull`).                                                                        | (no migration; code only) |
| `user`             | `UserResponse` DTO                                 | Adds `UUID especialidadId` and `String especialidadNombre` (nullable both).                                                           | (no migration; code only) |
| `user`             | `CreateAdminUserService`                           | Calls `GetSpecialtyByIdService` when `especialidadId` is provided; throws `SpecialtyNotFoundException` on miss.                       | (no migration; code only) |
| `patient-context`  | `paciente_contexto` table                          | Adds nullable `especialidad_id BINARY(16) NULL` with FK to `especialidades(id)` and an index.                                         | `V11`                   |
| `patient-context`  | `PacienteContextoEntity`                           | Adds `especialidad_id` scalar column (no `@ManyToOne`; mirrors how `paciente_id` is stored).                                          | (no migration; code only) |
| `patient-context`  | `PacienteContexto` domain model                    | Adds nullable `UUID especialidadId` field.                                                                                            | (no migration; code only) |
| `patient-context`  | `PacienteContextoResponse` DTO                     | Adds `UUID especialidadId`.                                                                                                           | (no migration; code only) |
| `patient-context`  | `AddPacienteContextoRequest` DTO                   | **Unchanged.** Specialty is inherited silently from `AuthenticatedUserContext`, not from the request body.                            | (no change)             |
| `patient-context`  | `AddPacienteContextoService`                       | Reads `caller.user.especialidadId` (may be null) and passes it to the domain model.                                                   | (no migration; code only) |
| `article`          | `articulos_cientificos` table                      | Adds nullable `especialidad_id BINARY(16) NULL` with FK to `especialidades(id)` and an index.                                         | `V11`                   |
| `article`          | `ArticleEntity`                                    | Adds `@ManyToOne(LAZY)` to `SpecialtyEntity` on `especialidad_id`; new `@NamedEntityGraph("Article.withTagsAndSpecialty")`.            | (no migration; code only) |
| `article`          | `Article` domain model                             | Adds nullable `UUID especialidadId` field.                                                                                            | (no migration; code only) |
| `article`          | `ArticleResponse` DTO                              | Adds `UUID especialidadId` and `String especialidadNombre` (nullable both).                                                           | (no migration; code only) |
| `article`          | `CreateArticleRequest` / `POST /api/articles` flow | **Unchanged.** Manual create does not set the specialty — that is the `article-ai-analysis` feature's job. New rows without AI analysis simply have `especialidad_id = NULL`. | (no change)             |

The new bulk endpoint `POST /api/patients/{patientId}/contextos/bulk` and the matching-query filter (`AND a.especialidad_id = c.especialidad_id`) are introduced by features 3 and 4 — they are not in scope for this spec.

### Database-side enforcement (from `database-objects`)

The [`12-05-2026-database-objects/`](../12-05-2026-database-objects/) spec adds a MySQL-only `BEFORE UPDATE` trigger `tr_specialty_soft_delete_guard` that rejects a soft-delete (`activo` TRUE → FALSE) while any article or `paciente_contexto` row still references the specialty. The trigger lives in `db/vendor_mysql/V13__add_database_objects.sql` and complements `SoftDeleteSpecialtyService` — the application service still runs, but on MySQL a soft-delete with dangling references surfaces as a `SQLException` from the JDBC layer (currently mapped to 500 by `FallbackMapper`; a clean 409 mapping is deferred). On H2 dev / `@QuarkusTest`, the trigger is absent and the dangling-reference case is silent — the application is expected to call this out via the COO UI before invoking the delete.

> **Note.** The existing `specs/1-05-2026-<feature>/` spec remains the canonical record for unchanged behavior; modified behavior will be re-snapshotted as `specs/<implementation-date>-<feature>/` when this work ships.

## Key technical decisions

### 1. `slug` as a first-class field, not derived

The vocabulary loader in feature 2 keys JSON files by `<slug>.json`. Deriving the slug server-side (e.g. lowercase + diacritics-strip + replace spaces) is brittle: a future rename or migration of `nombre` would silently break the file resolution. We expose `slug` as an explicit field the COO sets at creation time. Once set, it is editable via `PUT`, but the migration / deploy story is the COO's responsibility (the JSON file must be moved or renamed in the same release).

### 2. Asymmetric uniqueness: `nombre` (active-only) vs `slug` (global)

`nombre` reuse after soft-delete is allowed — it is a human-facing label and a deleted specialty's name is fair game for a future replacement. `slug` reuse is forbidden across all rows because:

- The `slug` backs a filesystem path (`src/main/resources/vocabulary/<slug>.json`).
- A deleted specialty's JSON file may remain in the repo until a cleanup pass.
- Re-using a slug could silently bind a new specialty to a stale vocabulary.

The DB enforces `slug` global uniqueness via `UNIQUE`. `nombre` active-uniqueness is enforced at the service layer because `@SQLRestriction` would hide soft-deleted rows from a hypothetical DB unique constraint anyway — making it a service-only concern is consistent.

### 3. Soft delete preserves cross-feature references

Soft-deleting a specialty **does not** null out `especialidad_id` on the three referencing tables. A user, a context entry, or an article that was tagged with `Cardiología` retains that link even if `Cardiología` is later soft-deleted. Justifications:

- Historic queries ("what specialties did this article use to be classified under?") remain answerable.
- The matching-query filter in `article-ai-analysis` joins on `especialidad_id` equality — equality still resolves even when the row is soft-deleted (the join doesn't go through `@SQLRestriction`-filtered `SELECT … FROM especialidades`).
- Listing endpoints exclude soft-deleted specialties from the UI, which is the only place users encounter the catalog. Backend joins are unaffected.

The trade-off: a soft-deleted specialty appears as a dangling reference in admin UIs that try to render the name. The fix is to look up the row including soft-deleted ones (via a separate repository method) when rendering historic data — out of scope for this spec.

### 4. Specialty assignment is immutable on `User`

No `PATCH /api/admin/users/{id}` exists. Doctors are tied to the specialty assigned at creation time. Rationale:

- Mutating a doctor's specialty silently re-scopes all their future context entries — a security-adjacent concern (a doctor "promoted" to Oncología could attach oncology vocabulary to records that were entered under Cardiología scope).
- The MVP does not yet have audit logging to make such a transition reviewable.
- A future re-assignment endpoint would need a dedicated spec (with audit trail and downstream re-tagging policy).

### 5. Nullable `especialidad_id` on `User` (no backfill)

Existing users seeded before this feature lands have no specialty. We do **not** backfill. Reasons:

- The seed `COO` is a non-clinical role — assigning it a specialty makes no sense.
- The pre-existing test fixtures and demo accounts would need a default specialty, which adds ambiguity.
- The AI features degrade gracefully when the caller has no specialty: the analyze endpoints return 400 with a clear message, rather than silently using a wrong vocabulary.

The 400 behavior is detailed in `consulta-ai-analysis/design.md` (feature 4).

### 6. Three migrations (V9, V10, V11), one concern each

Per [conventions/migrations.md](../../conventions/migrations.md#golden-rules):

- V9 — schema only (CREATE TABLE).
- V10 — seed data (idempotent `ON DUPLICATE KEY UPDATE`).
- V11 — cross-feature schema changes (add FK columns to existing tables).

Splitting prevents an environment with a corrupt seed (e.g. a deleted seed row) from blocking the FK migration on existing tables.

### 7. No `@NamedEntityGraph` on `SpecialtyEntity`

Specialty has no relations — it is the target of FKs, not the owner. The graph rule (always use graphs for relations) does not apply. Other entities (`UserEntity`, `ArticleEntity`) define their own `with…AndSpecialty` graphs and own the JOIN FETCH.

### 8. COO is the sole admin

Mirrors the `user` admin pattern (`CreateAdminUserService` already gates on `COO`). We considered introducing a `CATALOG_ADMIN` role to delegate specialty CRUD, but:

- Adds a role-seeding migration without a current use case.
- Specialty creation is a low-frequency operation (per institution onboarding).
- `COO` is already the role with global control of the platform.

The role-mismatch error reuses `RoleMismatchException` (already mapped to 403).

### 9. `GetSpecialtyByIdService` as the shared existence check

Other features that need to validate an `especialidadId` (user-admin in this spec, plus the future bulk endpoint in feature 4) call `GetSpecialtyByIdService.execute(id)` rather than `SpecialtyRepository.findByUuid(...)` directly. Rationale:

- The service raises `SpecialtyNotFoundException` (mapped to 404) — consistent error code at every call site.
- Cross-feature dependency is on the use-case interface, not the port — keeps the dependency direction Clean-Architecture-compliant.
- Future caching can be added inside the service without touching callers.

### 10. Listing is alphabetic, no pagination

The catalog is ~15-30 rows. Sort order in the response is `nombre ASC` (Spanish locale collation — `Cardiología` before `Endocrinología`). No `?page` / `?size` parameters — the frontend renders the full list as a dropdown.

## Open technical decisions / risks

- **Locale-sensitive sort.** MySQL default collation `utf8mb4_unicode_ci` orders Spanish accents correctly; H2 in `MODE=MySQL` may not. If integration tests on H2 show a different order than MySQL, we will switch to client-side sort in the service rather than rely on `ORDER BY nombre`. Tracked as a risk; not a blocker.
- **Vocabulary file ↔ slug coupling.** The `medical-vocabulary` feature has no DB representation, so a renamed slug must coincide with a renamed JSON file. We document the workflow in feature 2's spec; this feature's `UpdateSpecialtyService` does not (and cannot) check filesystem state.
- **Soft-deleted specialty in historic queries.** Admin UIs rendering historic article tags or context entries may need a "show all (including deleted)" mode. Out of scope here; flagged for a future patient-context / article UI spec.
