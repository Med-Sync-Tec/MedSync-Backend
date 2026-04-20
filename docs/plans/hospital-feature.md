# Plan: Feature `hospital` — Integración con la BD externa del hospital

## Contexto

MedSync es una plataforma de farmacovigilancia que cruza artículos científicos con el contexto clínico de pacientes. Parte de ese contexto clínico vive en **el sistema del hospital** (una BD externa que MedSync no administra): expedientes clínicos y consultas médicas en formato SOAP.

Este feature permite a MedSync **leer** expedientes y consultas desde esa BD externa y exponerlos vía REST, conectándolos con los pacientes de MedSync a través del campo `patient.expedienteExternoId`.

La BD del hospital se **simula** con un segundo contenedor MySQL (`hospital-db`), independiente del contenedor de MedSync. Quarkus se configura con **dos datasources nombrados**: el default (MedSync) y `hospital` (read-only).

Este feature establece el patrón para todos los **gateways** futuros (según CLAUDE.md: *"Gateway → servicios o datos externos"*), distinguiéndolos de los `*Repository` que manejan datos propios de MedSync.

## Alcance

- **Sí**: leer expedientes y consultas del hospital, exponerlos vía REST, vincularlos con pacientes de MedSync.
- **No**: escribir a la BD del hospital, autenticación con el hospital, sincronización en tiempo real, seed de datos (lo poblará otro agente aparte).

## Decisiones clave

1. **Segunda BD MySQL simulada** con Docker Compose (`hospital-db`, puerto host `3308` → interno `3306`). Contenedor aislado del `medsync-db` existente.
2. **Dos datasources en Quarkus**: `<default>` (MedSync, ya configurado) + `hospital` (nuevo, read-only).
3. **Schema del hospital definido en init scripts de Docker** (`docker/hospital-initdb/01_schema.sql`). El hospital administra su schema — MedSync NO lo controla con Flyway.
4. **Seed de datos lo hará otro agente** posteriormente (no incluido en este plan). Por ahora basta con el schema vacío para que Hibernate valide.
5. **Feature único `hospital`** (no dos features separados) con dos modelos de dominio: `ExpedienteClinico` y `Consulta`.
6. **Gateway, no Repository** — la interfaz se llama `HospitalGateway` porque va a un sistema externo. Vive en `domain/hospital/repository/` (por convención del CLAUDE.md, `repository/` aloja ambos tipos).
7. **Read-only**: `HospitalGateway` solo tiene métodos `find*`, ningún `save/update/delete`.
8. **Conexión Patient ↔ Hospital** vía `patient.expedienteExternoId` (varchar) ↔ `expedientes_clinicos.paciente_externo_id`. No hay FK (BDs distintas), solo convención.
9. **Endpoints anidados** bajo `/api/patients/{id}/...` para mejor UX del frontend. Un endpoint extra para detalle de consulta por id.
10. **TDD completo** siguiendo la misma metodología que el feature `patient` (tests unitarios puros para dominio, Mockito para services, `@QuarkusTest` para gateway y REST).
11. **Hibernate en `validate`** para el datasource del hospital también. Si las `@Entity` no coinciden con el schema del init script, la app no arranca.
12. **Transacciones independientes por datasource** — no usamos XA. Si un flujo lee de ambas BDs, son dos transacciones separadas. Aceptable para read-only.

## Endpoints

```
GET /api/patients/{uuid}/expediente        → ExpedienteClinicoResponse | 404
GET /api/patients/{uuid}/consultas         → List<ConsultaResponse>
GET /api/consultas/{consultaId}            → ConsultaResponse | 404
```

- `{uuid}` es el id del paciente en MedSync (BINARY(16) UUID).
- `{consultaId}` es el id de la consulta en el hospital (varchar).
- `404` si el paciente no existe **o** si el paciente existe pero no tiene expediente en el hospital.
- `GET /consultas` devuelve `200` con lista vacía si el paciente existe pero no tiene consultas aún.

## Archivos a crear / modificar

### Infraestructura Docker

- **NEW** `docker/hospital-initdb/01_schema.sql` — `CREATE TABLE expedientes_clinicos` + `CREATE TABLE consultas`. Schema exacto abajo.
- **MOD** `docker-compose.yml` — agregar servicio `hospital-db` con puerto `3308:3306`, volumen persistente `hospital-db-data`, healthcheck, mount del init dir.

### Configuración

- **MOD** `src/main/resources/application.properties` — agregar:
  - Datasource `hospital` con url `jdbc:mysql://localhost:3308/hospital`, credenciales.
  - Persistence unit `hospital` con `quarkus.hibernate-orm.hospital.packages=itesm.medsync.infrastructure.hospital` y `database.generation=validate`.
  - Solo para profile `%mysql-local` (dev con H2 puede quedar sin el hospital por simplicidad, o crear H2 separado).

### Domain `domain/hospital/`

- **NEW** `model/ExpedienteClinico.java` — POJO inmutable. Campos: `id` (String), `pacienteExternoId` (String), `doctorResponsableId` (String, nullable), `createdAt`, `updatedAt` (LocalDateTime). Constructor con validaciones: `id` y `pacienteExternoId` no null ni blank.
- **NEW** `model/Consulta.java` — POJO inmutable. Campos: `id` (String), `expedienteId` (String), `fecha` (LocalDateTime), `motivoConsulta`, `subjetivo`, `objetivo`, `evaluacion`, `plan`, `prescripcion`, `diagnostico` (todos String, nullables excepto id/expedienteId/fecha). Constructor valida campos requeridos.
- **NEW** `repository/HospitalGateway.java` — interface con:
  - `Optional<ExpedienteClinico> findExpedienteByPacienteExternoId(String pacienteExternoId)`
  - `List<Consulta> findConsultasByPacienteExternoId(String pacienteExternoId)`
  - `Optional<Consulta> findConsultaById(String consultaId)`
- **NEW** `usecase/GetExpedienteByPatientUseCase.java` — `ExpedienteClinico execute(UUID patientId)`
- **NEW** `usecase/GetConsultasByPatientUseCase.java` — `List<Consulta> execute(UUID patientId)`
- **NEW** `usecase/GetConsultaByIdUseCase.java` — `Consulta execute(String consultaId)`
- **NEW** `exception/ExpedienteNotFoundException.java` — cuando un paciente no tiene expediente en el hospital. Mapea a 404.
- **NEW** `exception/ConsultaNotFoundException.java` — cuando se pide una consulta que no existe. Mapea a 404.
- **NEW** `exception/InvalidHospitalDataException.java` — para validaciones del dominio.

### Application `application/hospital/`

- **NEW** `GetExpedienteByPatientService.java` — `@ApplicationScoped`, implementa `GetExpedienteByPatientUseCase`. Flujo:
  1. Inyecta `GetPatientByIdUseCase` (del feature patient) + `HospitalGateway`
  2. `patient = getPatientByIdUseCase.execute(patientId)` → puede tirar `PatientNotFoundException`
  3. `gateway.findExpedienteByPacienteExternoId(patient.getExpedienteExternoId())` → si vacío, tira `ExpedienteNotFoundException`
- **NEW** `GetConsultasByPatientService.java` — similar: obtiene patient, luego `gateway.findConsultasByPacienteExternoId(...)`. Devuelve lista vacía si no hay, NO tira excepción (un paciente puede existir sin consultas).
- **NEW** `GetConsultaByIdService.java` — `gateway.findConsultaById(id).orElseThrow(ConsultaNotFoundException::new)`.

### Infrastructure `infrastructure/hospital/`

- **NEW** `ExpedienteClinicoHospitalEntity.java` — `@Entity @Table(name="expedientes_clinicos")`. Campos escalares con `@Column`. Nota: id es varchar (String), no UUID. Package `itesm.medsync.infrastructure.hospital` — debe coincidir con `quarkus.hibernate-orm.hospital.packages`.
- **NEW** `ConsultaHospitalEntity.java` — `@Entity @Table(name="consultas")`. Relación con expediente: `expedienteId` como String escalar (NO `@ManyToOne` — queremos control explícito de queries, y el hospital no necesita navegación de objetos).
- **NEW** `HospitalPersistenceMapper.java` — métodos estáticos `toDomain(ExpedienteClinicoHospitalEntity)` y `toDomain(ConsultaHospitalEntity)`.
- **NEW** `HospitalGatewayImpl.java` — `@ApplicationScoped`, implementa `HospitalGateway`. Inyecta `EntityManager` del persistence unit `hospital` con `@PersistenceContext(unitName="hospital")`. Usa queries JPQL:
  - `findExpedienteByPacienteExternoId`: `SELECT e FROM ExpedienteClinicoHospitalEntity e WHERE e.pacienteExternoId = :id`
  - `findConsultasByPacienteExternoId`: JOIN implícito vía subquery o JPQL con JOIN explícito:
    ```
    SELECT c FROM ConsultaHospitalEntity c
    WHERE c.expedienteId IN (
      SELECT e.id FROM ExpedienteClinicoHospitalEntity e
      WHERE e.pacienteExternoId = :id
    )
    ORDER BY c.fecha DESC
    ```
  - `findConsultaById`: `SELECT c FROM ConsultaHospitalEntity c WHERE c.id = :id`
- **MOD** `infrastructure/config/GlobalExceptionHandler.java` — agregar mappers para `ExpedienteNotFoundException` (404), `ConsultaNotFoundException` (404), `InvalidHospitalDataException` (400).

### Interfaces `interfaces/rest/hospital/`

- **NEW** `ExpedienteClinicoResponse.java` — record con todos los campos del dominio.
- **NEW** `ConsultaResponse.java` — record con campos SOAP expandidos (subjetivo, objetivo, evaluacion, plan) + prescripción, diagnóstico, fecha, motivo.
- **NEW** `ConsultaSummaryResponse.java` (opcional) — record más ligero con solo `id`, `fecha`, `motivoConsulta`, `diagnostico` para el listado. Decisión: empezar sin este; si el listado pesa mucho en el frontend, agregarlo después.
- **NEW** `HospitalRestMapper.java` — `toResponse(ExpedienteClinico)`, `toResponse(Consulta)`, `toResponseList(List<Consulta>)`.
- **NEW** `HospitalResource.java` — `@Path("/api")`. Endpoints:
  - `GET /patients/{id}/expediente`
  - `GET /patients/{id}/consultas`
  - `GET /consultas/{consultaId}`
  Inyecta los 3 UseCases. Anotaciones OpenAPI (`@Tag`, `@Operation`, `@APIResponse`) para Swagger.

### Tests (TDD completo)

Mismo árbol de paquetes en `src/test/java/...`:

**Dominio (JUnit puro):**
- `ExpedienteClinicoTest` — constructor con/sin campos requeridos, inmutabilidad, equals/hashCode por id.
- `ConsultaTest` — constructor, campos requeridos, equals/hashCode por id.

**Application (Mockito):**
- `GetExpedienteByPatientServiceTest` — paciente existe + expediente existe → devuelve; paciente existe sin expediente → `ExpedienteNotFoundException`; paciente no existe → propaga `PatientNotFoundException`.
- `GetConsultasByPatientServiceTest` — paciente existe con consultas → devuelve lista; paciente existe sin consultas → lista vacía; paciente no existe → `PatientNotFoundException`.
- `GetConsultaByIdServiceTest` — existe → devuelve; no existe → `ConsultaNotFoundException`.

**Infrastructure (`@QuarkusTest` + `@TestTransaction`):**
- `HospitalGatewayImplTest` — inserta datos fixture directamente en la BD del hospital (o usa un dataset de prueba), luego verifica queries. Necesita que el test profile apunte al `hospital-db` de Docker O use H2 separado para el hospital en `%test`.

**Integración REST (`@QuarkusTest` + RestAssured):**
- `HospitalResourceIT` — cubre:
  - `GET /patients/{uuid}/expediente` con paciente + expediente en seed → 200
  - `GET /patients/{uuid}/expediente` con paciente sin expediente → 404
  - `GET /patients/{uuid-inexistente}/expediente` → 404
  - `GET /patients/{uuid}/consultas` con consultas → 200 + lista no vacía
  - `GET /patients/{uuid}/consultas` sin consultas → 200 + `[]`
  - `GET /consultas/{id}` existente → 200
  - `GET /consultas/{id-inexistente}` → 404

## Schema de la BD del hospital

Archivo: `docker/hospital-initdb/01_schema.sql`

```sql
CREATE DATABASE IF NOT EXISTS hospital;
USE hospital;

CREATE TABLE expedientes_clinicos (
    id VARCHAR(50) NOT NULL,
    paciente_externo_id VARCHAR(100) NOT NULL,
    doctor_responsable_id VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_expedientes_clinicos PRIMARY KEY (id),
    CONSTRAINT uq_expedientes_paciente UNIQUE (paciente_externo_id)
);

CREATE INDEX idx_expedientes_paciente ON expedientes_clinicos (paciente_externo_id);

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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_consultas PRIMARY KEY (id),
    CONSTRAINT fk_consultas_expediente FOREIGN KEY (expediente_id)
        REFERENCES expedientes_clinicos(id)
);

CREATE INDEX idx_consultas_expediente ON consultas (expediente_id);
CREATE INDEX idx_consultas_fecha ON consultas (fecha);
```

**Nota**: la FK dentro de la BD del hospital SÍ existe (ambas tablas viven ahí). La FK cross-BD `patients.expediente_externo_id → expedientes_clinicos.paciente_externo_id` NO existe porque están en BDs separadas.

## Configuración de datasources en `application.properties`

Bloque a agregar al profile `%mysql-local`:

```properties
# Datasource secundario: hospital externo (read-only)
%mysql-local.quarkus.datasource.hospital.db-kind=mysql
%mysql-local.quarkus.datasource.hospital.jdbc.url=jdbc:mysql://localhost:3308/hospital
%mysql-local.quarkus.datasource.hospital.username=hospital
%mysql-local.quarkus.datasource.hospital.password=hospital

# Persistence unit para el datasource hospital
%mysql-local.quarkus.hibernate-orm.hospital.datasource=hospital
%mysql-local.quarkus.hibernate-orm.hospital.packages=itesm.medsync.infrastructure.hospital
%mysql-local.quarkus.hibernate-orm.hospital.database.generation=validate
%mysql-local.quarkus.hibernate-orm.hospital.dialect=org.hibernate.dialect.MySQLDialect
```

Nota: el `<default>` datasource queda intacto (MedSync). El default persistence unit debe excluir el package `itesm.medsync.infrastructure.hospital` para no mezclar entities. Esto se logra añadiendo a las props globales del default PU: `quarkus.hibernate-orm.packages=itesm.medsync.infrastructure.persistence` (es decir, solo las entities de MedSync).

## Cambios al `docker-compose.yml`

Agregar como servicio adicional (el `medsync-db` existente queda intacto):

```yaml
  hospital-db:
    image: mysql:8.0
    container_name: hospital-db
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: hospital
      MYSQL_USER: hospital
      MYSQL_PASSWORD: hospital
    ports:
      - "3308:3306"
    volumes:
      - hospital-db-data:/var/lib/mysql
      - ./docker/hospital-initdb:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot"]
      interval: 5s
      timeout: 3s
      retries: 10
      start_period: 20s
```

Y al bloque de volumes al final:

```yaml
volumes:
  medsync-db-data:
  hospital-db-data:
```

## Riesgos / gotchas

1. **Separación de packages por datasource** — cada `@Entity` va a UNO de los dos persistence units. Si Hibernate escanea el mismo package para ambos PUs, truena. Solución: `quarkus.hibernate-orm.packages` explícito en el default, y `quarkus.hibernate-orm.hospital.packages` explícito en el hospital. Paquetes separados: `infrastructure.persistence.*` (MedSync) vs `infrastructure.hospital.*` (hospital).

2. **Transacciones no atómicas entre BDs** — si un flujo lee de MedSync y del hospital, son dos transacciones. Si la segunda falla, la primera ya leyó. No hay rollback conjunto. Aceptable para read-only — si en el futuro se requiere escritura coordinada, evaluar XA (narayana-jta-objectstore).

3. **Tests de `HospitalGatewayImpl`** — necesitan una BD accesible. Opciones:
   - Levantar `hospital-db` antes de correr tests (asumir Docker corriendo).
   - Usar Testcontainers (agregar dependencia, el test levanta el contenedor solo).
   - H2 separado con `MODE=MySQL` para el PU `hospital` solo en profile `%test`.
   Recomendación: **H2 separado en `%test`** para que los tests corran sin Docker. Agregar config de test para PU `hospital` apuntando a `jdbc:h2:mem:hospital-test;MODE=MySQL`. Nota: esto significa que los tests validan que el SQL JPQL sea compatible pero no que la sintaxis MySQL-específica funcione — aceptable porque el JPQL es portable.

4. **Conexión al hospital caída** — si `hospital-db` no responde, los endpoints de este feature fallan. Para dev no importa. Para producción, el `GlobalExceptionHandler` debería mapear excepciones de JDBC a 503. Por ahora, fallback genérico 500. Documentado como deuda técnica.

5. **FK entre tablas del hospital** — existe internamente (`consultas.expediente_id → expedientes_clinicos.id`), lo cual es correcto. No afecta el gateway.

6. **Varchar como ID** — los IDs de expediente y consulta son varchar según el esquema ("ID externo — simulado"). NO son UUIDs. Respetarlo en entity y domain.

7. **Sin seed** — al arrancar el feature sin datos seed, todos los endpoints devolverán 404 o lista vacía. Esperado. El seed se agregará como paso posterior con otro agente.

## Orden de implementación (TDD por fase)

1. **Infra Docker + config** (no TDD, setup)
   - Crear `docker/hospital-initdb/01_schema.sql`
   - Modificar `docker-compose.yml` con `hospital-db`
   - Modificar `application.properties` con datasource + PU `hospital`
   - Verificar: `docker compose up -d` levanta ambos contenedores healthy, arranca Quarkus sin errores de validación Hibernate.

2. **Domain** (TDD)
   - Escribir `ExpedienteClinicoTest` + `ConsultaTest` (RED)
   - Implementar `ExpedienteClinico`, `Consulta`, 3 exceptions (GREEN)
   - Crear `HospitalGateway` interface + 3 UseCases

3. **Application** (TDD)
   - Escribir los 3 `*ServiceTest` con Mockito sobre `HospitalGateway` y `GetPatientByIdUseCase` (RED)
   - Implementar los 3 services (GREEN)

4. **Infrastructure** (TDD parcial)
   - Escribir `HospitalGatewayImplTest` con fixtures vía EntityManager directo (RED)
   - Implementar entities, mapper, `HospitalGatewayImpl` (GREEN)
   - Verificar Hibernate validate contra el schema real

5. **REST** (TDD)
   - Escribir `HospitalResourceIT` con fixtures en BD (RED)
   - Implementar DTOs, mapper, Resource, ampliar `GlobalExceptionHandler` (GREEN)

6. **Verificación final**
   - `./mvnw test` todo verde
   - `docker compose up -d` + `./mvnw quarkus:dev -Dquarkus.profile=mysql-local`
   - Swagger UI lista los 3 endpoints nuevos
   - Manualmente: levantar datos con otro agente, probar flujo patient → expediente → consultas

## Verificación end-to-end

1. `./mvnw compile` sin errores ni warnings
2. `./mvnw test` todos los tests pasan (existentes del feature patient + nuevos del feature hospital). Cobertura ≥80% en `domain/hospital/` y `application/hospital/`.
3. `docker compose up -d`:
   - `medsync-db` healthy
   - `hospital-db` healthy, con el schema cargado (`docker exec hospital-db mysql -u hospital -phospital hospital -e "SHOW TABLES;"` devuelve `expedientes_clinicos` y `consultas`)
4. `./mvnw quarkus:dev -Dquarkus.profile=mysql-local`:
   - Flyway aplica migraciones en `medsync-db`
   - Hibernate valida entities en AMBOS persistence units sin errores
   - `/q/swagger-ui` muestra los 3 endpoints nuevos bajo el tag `Hospital`
5. Después de que otro agente popule la BD del hospital con seed:
   - Crear un paciente en MedSync con `expedienteExternoId` que coincida con un seed del hospital
   - `GET /api/patients/{uuid}/expediente` → 200 con el expediente
   - `GET /api/patients/{uuid}/consultas` → 200 con lista de consultas SOAP
   - `GET /api/consultas/{id}` → 200 con detalle
   - Con IDs inexistentes → 404 correctos
6. Verificar imports prohibidos:
   - `domain/hospital/` sin `jakarta.persistence`, `jakarta.ws.rs`, `io.quarkus`, `jakarta.validation`
   - `application/hospital/` solo importa de `itesm.medsync.domain.*` y CDI (`jakarta.enterprise`, `jakarta.inject`)

## Archivos críticos durante la implementación

- `CLAUDE.md` (fuente de verdad de convenciones)
- `docs/features/patient.md` (patrón a replicar)
- `src/main/resources/application.properties` (config de los dos datasources)
- `docker-compose.yml` (dos contenedores MySQL coordinados)
- `docker/hospital-initdb/01_schema.sql` (schema del hospital)
- `src/main/java/itesm/medsync/infrastructure/hospital/HospitalGatewayImpl.java` (corazón técnico del feature)
- `src/main/java/itesm/medsync/infrastructure/config/GlobalExceptionHandler.java` (mapear las nuevas excepciones)

## Fuera de alcance (intencional)

- Seed de datos en la BD del hospital (otro agente lo hará aparte)
- Autenticación o autorización
- Paginación, filtros, búsqueda de consultas
- Escritura a la BD del hospital (POST/PUT/DELETE) — el hospital es dueño
- Cache de respuestas del hospital
- Reintentos ante fallos de conexión al hospital
- Feature flags para encender/apagar la integración hospital

## Próximos features relacionados

- `alert` — cruzará consultas + medicamentos + artículos para generar alertas. Consumirá el `HospitalGateway`.
- `medication` — aprovecha la prescripción de las consultas para detectar medicamentos obsoletos.
