# Feature: Hospital

Integración con la BD externa del hospital. Lee expedientes clínicos y consultas SOAP; permite **crear** consultas (con auto-creación de expediente si el paciente no tiene uno).

Establece el patrón **Gateway** de Clean Architecture para sistemas externos — distinto del `*Repository` que maneja datos propios de MedSync.

## Endpoints

| Método | Ruta | Propósito | Status codes |
|--------|------|-----------|--------------|
| `GET` | `/api/patients/{id}/expediente` | Obtener expediente clínico del paciente | 200 / 404 |
| `GET` | `/api/patients/{id}/consultas` | Listar consultas SOAP del paciente (ordenadas DESC por fecha) | 200 / 404 |
| `GET` | `/api/consultas/{consultaId}` | Obtener detalle de una consulta | 200 / 404 |
| `POST` | `/api/patients/{id}/consultas` | Crear consulta (auto-crea expediente si no existe) | 201 / 400 / 404 |

Swagger UI: `http://localhost:8080/q/swagger-ui` (tag `Hospital`).

## Arquitectura: dos datasources

MedSync se conecta a **dos BDs MySQL** distintas:

- **`<default>`** → BD de MedSync (pacientes, usuarios, alertas). Administrada por MedSync vía Flyway.
- **`hospital`** → BD externa del hospital (expedientes, consultas). MedSync NO controla su schema con Flyway — solo valida que sus `@Entity` coincidan con el schema existente.

```
┌──────────────────┐        ┌──────────────────┐
│  medsync-db      │        │  hospital-db     │
│  :3307 (prod)    │        │  :3308 (prod)    │
│                  │        │                  │
│  patients        │        │  expedientes_    │
│  users           │        │    clinicos      │
│  alerts          │        │  consultas       │
└────────┬─────────┘        └────────┬─────────┘
         │                           │
         │ PU <default>              │ PU "hospital"
         ▼                           ▼
    ┌────────────────────────────────────────┐
    │       Quarkus / Hibernate              │
    └────────────────────────────────────────┘
```

Dos persistence units en Quarkus, dos sets de `@Entity` en paquetes disjuntos:
- `itesm.medsync.infrastructure.persistence.*` → PU `<default>`
- `itesm.medsync.infrastructure.hospital.*` → PU `hospital`

## Modelo de dominio

### `ExpedienteClinico` (inmutable)

- `id: String` — varchar, NO UUID (lo define el hospital o MedSync si auto-crea)
- `pacienteExternoId: String` — identificador del paciente en el sistema del hospital. Conecta con `Patient.expedienteExternoId`.
- `doctorResponsableId: String` — opcional, médico del hospital
- `createdAt`, `updatedAt: LocalDateTime`

Reglas validadas en el constructor:
- `id` no null ni blank
- `pacienteExternoId` no null ni blank

Factory `ExpedienteClinico.create(pacienteExternoId, doctorResponsableId)` genera un UUID string como id y deja timestamps en null.

### `Consulta` (inmutable)

Estructura SOAP + prescripción + diagnóstico:

- `id: String`
- `expedienteId: String` — FK lógico al expediente
- `fecha: LocalDateTime` — obligatoria
- `motivoConsulta: String` — opcional
- `subjetivo: String` — opcional (S de SOAP)
- `objetivo: String` — opcional (O de SOAP)
- `evaluacion: String` — opcional (A / Assessment)
- `plan: String` — opcional (P de SOAP)
- `prescripcion: String` — opcional
- `diagnostico: String` — opcional
- `createdAt`, `updatedAt: LocalDateTime`

Reglas validadas en el constructor:
- `id` no null ni blank
- `expedienteId` no null ni blank
- `fecha` no null

Factory `Consulta.create(expedienteId, fecha, ...campos SOAP)` genera UUID string.

## Reglas de negocio

### Read (GET)

- Obtener expediente: si paciente no existe en MedSync → `404 PatientNotFoundException`. Si paciente existe pero no tiene expediente en hospital → `404 ExpedienteNotFoundException`.
- Listar consultas: si paciente no existe → 404. Si existe y no tiene consultas → `200 []` (lista vacía, NO 404).
- Detalle de consulta: si no existe → `404 ConsultaNotFoundException`.

### Write (POST)

- **Paciente requerido**: si el `patientId` no existe en MedSync → `404`. No se toca la BD del hospital.
- **Auto-creación de expediente**: si el paciente existe pero no tiene expediente en el hospital, MedSync crea uno automáticamente (solo con `pacienteExternoId`, sin `doctorResponsableId`). Esto es la **única forma** en que MedSync escribe en `expedientes_clinicos`.
- **ID generado en backend**: el cliente nunca envía `consultaId` — MedSync genera un UUID string.
- **Sin edición ni borrado**: `PUT`/`DELETE` de consultas fuera de alcance.

## Validaciones en dos niveles

| Nivel | Dónde vive | Qué valida |
|-------|------------|------------|
| **Formato** | `CreateConsultaRequest` (capa `interfaces/rest`) | `@NotNull fecha`, `@Size(max=...)` en todos los campos de texto |
| **Negocio** | Constructor de `Consulta` y `ExpedienteClinico` (capa `domain`) | id/expedienteId/pacienteExternoId no blank, fecha no null |

La redundancia es intencional: el dominio no confía en quien lo invoque.

## DTOs (capa `interfaces/rest/hospital/`)

- `CreateConsultaRequest` — entrada del POST. Campos `public` con anotaciones Jakarta Bean Validation. Sin `consultaId` (lo genera el backend).
- `ExpedienteClinicoResponse` — record inmutable, 5 campos.
- `ConsultaResponse` — record inmutable, 12 campos.

El dominio nunca se expone directamente — siempre pasa por `HospitalRestMapper.toResponse(...)`.

## Arquitectura de capas

```
┌────────────────────────────────────────────────────────────────┐
│ interfaces/rest/hospital/                                      │
│   HospitalResource  @Path("/api/patients/{id}")                │
│     ├── GET /expediente                                        │
│     ├── GET /consultas                                         │
│     └── POST /consultas                                        │
│   HospitalConsultaResource  @Path("/api/consultas")            │
│     └── GET /{consultaId}                                      │
│   DTOs: CreateConsultaRequest, ConsultaResponse,               │
│          ExpedienteClinicoResponse                             │
│   HospitalRestMapper                                           │
└──────────────────────────┬─────────────────────────────────────┘
                           │ recibe primitivos
                           ▼
┌────────────────────────────────────────────────────────────────┐
│ application/hospital/                                          │
│   GetExpedienteByPatientService  implements  *UseCase          │
│   GetConsultasByPatientService                                 │
│   GetConsultaByIdService                                       │
│   CreateConsultaService                                        │
│   (1 service por use case)                                     │
└──────────────────────────┬─────────────────────────────────────┘
                           │ depende de las interfaces del domain
                           ▼
┌────────────────────────────────────────────────────────────────┐
│ domain/hospital/                                               │
│   model/       ExpedienteClinico, Consulta (POJOs inmutables)  │
│   usecase/     4 interfaces *UseCase                           │
│   repository/  HospitalGateway (interface)                     │
│   exception/   ExpedienteNotFoundException,                    │
│                ConsultaNotFoundException,                      │
│                InvalidHospitalDataException                    │
│   ← núcleo puro, sin frameworks                                │
└──────────────────────────▲─────────────────────────────────────┘
                           │ implementa HospitalGateway
┌──────────────────────────┴─────────────────────────────────────┐
│ infrastructure/hospital/                                       │
│   ExpedienteClinicoHospitalEntity (@Entity)                    │
│   ConsultaHospitalEntity (@Entity)                             │
│   HospitalPersistenceMapper                                    │
│   HospitalGatewayImpl implements HospitalGateway               │
│     ← inyecta @PersistenceUnit("hospital") EntityManager       │
└────────────────────────────────────────────────────────────────┘
```

`HospitalGatewayImpl` NO extiende `PanacheRepositoryBase` porque maneja dos entities (`ExpedienteClinicoHospitalEntity` y `ConsultaHospitalEntity`). Usa `EntityManager` directo con JPQL.

## Decisiones técnicas

### 1. `HospitalGateway` en vez de `HospitalRepository`

Por convención de `CLAUDE.md`: los *Repositories* manejan datos propios de MedSync, los *Gateways* manejan sistemas externos. El hospital es un sistema externo — MedSync no es dueño de su schema.

### 2. `id` como `String` (varchar), no `UUID`

El hospital define sus propios IDs como `VARCHAR(50)`. Respetamos ese tipo. Cuando MedSync auto-crea un expediente o consulta, usa `UUID.randomUUID().toString()` (formato canónico, compatible con varchar).

### 3. Endpoints divididos en dos clases REST

El resource split no es estético — es **necesario**:

- `PatientResource` tiene `@Path("/api/patients")` (13 chars literales).
- Con `HospitalResource @Path("/api")`, JAX-RS elegía PatientResource al resolver `/api/patients/{id}/expediente` (más específico gana), y devolvía 404 "Unable to find matching target resource method" porque PatientResource no tiene ese subpath.
- Fix: `HospitalResource @Path("/api/patients/{id}")` (14 chars literales) → gana al resolver. `{id}` se captura a nivel de clase.
- Para `/api/consultas/{consultaId}` no hay conflicto, vive en clase separada `HospitalConsultaResource`.

### 4. Sin `@SQLRestriction` (no hay soft delete)

El hospital administra su ciclo de vida. MedSync solo lee (excepto la creación de consultas). No hay concepto de "expediente inactivo" en este feature.

### 5. JPQL con subquery en `findConsultasByPacienteExternoId`

```sql
SELECT c FROM ConsultaHospitalEntity c
WHERE c.expedienteId IN (
    SELECT e.id FROM ExpedienteClinicoHospitalEntity e
    WHERE e.pacienteExternoId = :pacExt
) ORDER BY c.fecha DESC
```

No se usa `@ManyToOne` entre entities. Razón: queries explícitas > navegación de objetos, y el hospital no requiere grafos densos.

### 6. Auto-creación de expediente

El plan original marcó "read-only en expedientes". Durante la implementación del POST, se aceptó relajar esto: si un doctor crea una consulta y no hay expediente, MedSync crea uno en vez de lanzar 404.

Justificación: desde el punto de vista del cliente (MedSync frontend), el expediente es un contenedor implícito — cada paciente "tiene" uno por definición. Exigir crear expediente explícitamente antes de la primera consulta es fricción innecesaria.

Riesgo: si el hospital implementa su propia política de creación de expedientes (p.ej. con metadata específica), MedSync la estaría brincando. Documentado como punto a revisar con el equipo del hospital.

Fix trivial si cambia la decisión: en `CreateConsultaService`, sustituir `.orElseGet(...)` por `.orElseThrow(() -> new ExpedienteNotFoundException(patientId))`.

### 7. Hibernate `drop-and-create` en `%test`, `validate` en `%mysql-local`

- En **tests**: `quarkus.hibernate-orm."hospital".database.generation=drop-and-create` sobre H2 en memoria. Hibernate genera el schema desde las `@Entity`. Sin Docker. Sin Flyway para hospital.
- En **mysql-local**: `validate`. Schema viene del init script de Docker (`docker/hospital-initdb/01_schema.sql`). Hibernate solo valida match.

Trade-off: los tests no validan sintaxis MySQL-específica del hospital, pero como usamos JPQL portable, es aceptable. Si se agregan native queries MySQL-only, habría que subir a Testcontainers.

### 8. Transacciones independientes por datasource

No usamos XA. Si un flujo lee de MedSync y del hospital, son dos transacciones separadas. Si la segunda falla, la primera ya leyó. Aceptable para este caso porque solo la escritura de consulta toca el hospital — y la lectura del paciente en MedSync no tiene efecto colateral.

### 9. Request fecha: cliente envía `LocalDateTime` ISO-8601

El cliente manda `"fecha": "2026-04-20T10:30:00"`. El servidor lo parsea a `LocalDateTime`. No se infiere el "ahora" del servidor — el doctor puede registrar consultas retroactivas (siempre y cuando pase la validación formal).

## Tests

| Archivo | Tipo | Cubre |
|---------|------|-------|
| `ExpedienteClinicoTest` | Unit (JUnit 5) | Constructor, validaciones, factory, equals/hashCode |
| `ConsultaTest` | Unit (JUnit 5) | Idem |
| `GetExpedienteByPatientServiceTest` | Unit (Mockito) | Paciente existe+expediente, paciente sin expediente, paciente inexistente |
| `GetConsultasByPatientServiceTest` | Unit (Mockito) | Con consultas, sin consultas, paciente inexistente |
| `GetConsultaByIdServiceTest` | Unit (Mockito) | Existe, no existe |
| `CreateConsultaServiceTest` | Unit (Mockito) | Happy path con expediente existente, auto-creación de expediente, paciente inexistente |
| `HospitalGatewayImplTest` | Integración (`@QuarkusTest` + `@TestTransaction`) | 3 finds + saveExpediente + saveConsulta |
| `HospitalResourceIT` | Integración REST (`@QuarkusTest` + RestAssured) | 14 escenarios: GETs felices/404, POST feliz con expediente, POST con auto-creación, POST sin fecha (400), POST mínimo, POST paciente inexistente |

Ejecutar:
- `./mvnw test` — tests unitarios y gateway integration (sin IT)
- `./mvnw verify -DskipITs=false` — incluye IT REST

## Schema del hospital

`docker/hospital-initdb/01_schema.sql` (aplicado automáticamente por Docker al arrancar `hospital-db`):

```sql
CREATE TABLE expedientes_clinicos (
    id VARCHAR(50) NOT NULL,
    paciente_externo_id VARCHAR(100) NOT NULL,
    doctor_responsable_id VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_expedientes_clinicos PRIMARY KEY (id),
    CONSTRAINT uq_expedientes_paciente UNIQUE (paciente_externo_id)
);

CREATE TABLE consultas (
    id VARCHAR(50) NOT NULL,
    expediente_id VARCHAR(50) NOT NULL,
    fecha DATETIME NOT NULL,
    motivo_consulta TEXT NULL,
    subjetivo TEXT NULL,
    objetivo TEXT NULL,
    evaluacion TEXT NULL,
    plan TEXT NULL,
    prescripcion TEXT NULL,
    diagnostico TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_consultas PRIMARY KEY (id),
    CONSTRAINT fk_consultas_expediente FOREIGN KEY (expediente_id)
        REFERENCES expedientes_clinicos(id)
);
```

**Nota**: la FK `consultas.expediente_id → expedientes_clinicos.id` vive dentro de la BD del hospital, correcto. La FK cross-BD `patients.expediente_externo_id → expedientes_clinicos.paciente_externo_id` NO existe (son BDs separadas) — se respeta por convención en el código.

## Configuración de datasources (resumen)

En `application.properties`, cuatro profiles configurados:

| Profile | default (MedSync) | hospital |
|---------|-------------------|----------|
| `%dev` | H2 in-memory | H2 in-memory, `drop-and-create` |
| `%test` | H2 in-memory + Flyway clean | H2 in-memory, `drop-and-create` |
| `%mysql-local` | `jdbc:mysql://localhost:3307/medsync` | `jdbc:mysql://localhost:3308/hospital`, `validate` |
| `%prod` | env vars `DB_*` | env vars `HOSPITAL_DB_*`, `validate` |

## Manejo de excepciones (`GlobalExceptionHandler`)

| Excepción | HTTP |
|-----------|------|
| `ExpedienteNotFoundException` | 404 |
| `ConsultaNotFoundException` | 404 |
| `InvalidHospitalDataException` | 400 |
| `PatientNotFoundException` (propagada desde el feature patient) | 404 |
| `jakarta.validation.ConstraintViolationException` (DTO inválido) | 400 |

## Fuera de alcance (intencional)

- **Edición de consultas** (`PUT`/`PATCH`)
- **Borrado de consultas** (`DELETE`)
- **Endpoint explícito de creación de expediente** — solo auto-creado
- **Autenticación / autorización** — cualquiera puede crear consultas hoy
- **Sincronización de cambios** en tiempo real desde el hospital (no hay webhooks ni polling)
- **Cache** de respuestas del hospital
- **Reintentos** ante fallos de conexión al hospital
- **Paginación / filtros** en el listado de consultas

## Deuda técnica reconocida

1. Excepciones JDBC de conexión caída al hospital → mapeadas a 500 genérico. Falta mapper específico → 503 Service Unavailable.
2. Tests del gateway sobre H2 `MODE=MySQL`, no MySQL real. Si se agregan native queries MySQL-específicas, migrar a Testcontainers.
3. Warning de Quarkus `quarkus.hibernate-orm.database.generation is deprecated` — refactor a `schema-management.strategy` cuando toque.
4. Auto-creación de expediente sin autorización del hospital — validar con el equipo del hospital antes de prod.
5. Sin autenticación — el endpoint POST es público. Debe bloquearse antes de deployment real.

## Siguientes features relacionados

- **`alert`** — cruzará las consultas (prescripciones + diagnósticos) con artículos científicos para generar alertas de farmacovigilancia. Consumirá `HospitalGateway.findConsultasByPacienteExternoId(...)`.
- **`medication`** — catálogo de medicamentos. Permitirá parsear `prescripcion` y detectar medicamentos obsoletos.
- **`user`** — usuarios (médicos, admin). Agregará FK `consultas.doctor_id → users.id` (en el dominio de MedSync si los médicos son de MedSync, o queda como varchar si son del hospital).
