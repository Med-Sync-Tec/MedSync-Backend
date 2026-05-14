# Feature: Solicitud Acceso — Implementation Tasks

> Status: **all tasks complete**. Preserved as a historical record of the implementation order.

## Domain

- [x] `EstadoSolicitud` enum (`PENDIENTE`, `APROBADO`, `RECHAZADO`)
- [x] `SolicitudAcceso` model — no password field; `rejectedAt`; `create()`, `aprobar()`, `rechazar()` methods
- [x] `AprobarSolicitudResult` record (`UserWithRole` + `passwordResetLink`)
- [x] `SolicitudAccesoRepository` interface — `save`, `findByToken`, `findByCorreo`, `delete`
- [x] `CrearSolicitudUseCase` interface — `execute(nombre, correo, rol)` (no password)
- [x] `AprobarSolicitudUseCase` interface — returns `AprobarSolicitudResult`
- [x] `RechazarSolicitudUseCase` interface — returns `SolicitudAcceso`
- [x] `SolicitudNotFoundException`
- [x] `SolicitudPendienteException` (409)
- [x] `SolicitudRechazadaRecientementeException` (429)
- [x] `FirebaseUserGateway` extended with `generatePasswordResetLink(String email)`
- [x] `CreateAdminUserUseCase` — removed `password` parameter

## Application

- [x] `CrearSolicitudService` — validates email not in `usuarios`, no existing `PENDIENTE`, cooldown check; deletes expired `RECHAZADO` record; `@ConfigProperty` cooldown
- [x] `AprobarSolicitudService` — state check; `createAdminUser` (no password); `generatePasswordResetLink`; `solicitud.aprobar()`; returns `AprobarSolicitudResult`
- [x] `RechazarSolicitudService` — state check; `solicitud.rechazar()`; returns `SolicitudAcceso`
- [x] `CreateAdminUserService` — generates 24-char `SecureRandom` password internally; never stored or returned

## Persistence

- [x] Migration `V11__solicitudes_acceso_security.sql` — DROP `password`, DROP UNIQUE `correo`, ADD `rejected_at`
- [x] `SolicitudAccesoEntity` — removed `password`, added `rejectedAt`
- [x] `SolicitudAccesoPersistenceMapper` — updated `toEntity` / `toDomain`
- [x] `SolicitudAccesoRepositoryImpl` — added `findByCorreo`; updated `save()` to persist `rejectedAt`
- [x] `FirebaseUserGatewayImpl` — implemented `generatePasswordResetLink`

## REST

- [x] `RegisterRequest` — removed `password` field; added `@Pattern(regexp = "DOCTOR|COO")` on `rol`
- [x] `AuthResource.register` — calls `crearSolicitud.execute(nombre, correo, rol)`; sends admin email + confirmation email
- [x] `SolicitudResource.aprobar` — uses `AprobarSolicitudResult`; sends approval email with reset link
- [x] `SolicitudResource.rechazar` — uses returned `SolicitudAcceso`; sends rejection email
- [x] Wire `SolicitudPendienteException` and `SolicitudRechazadaRecientementeException` into `GlobalExceptionHandler`
- [x] `FirebaseAuthFilter` — `"api/solicitudes/"` added to `PUBLIC_PATH_PREFIXES`

## Configuration

- [x] `application.properties` — added `medsync.solicitudes.rejection-cooldown-days=5`
- [x] `application.properties` — added `%dev.quarkus.mailer.mock=false`
- [x] `.env` — `MAILER_FROM`, `MAILER_PASSWORD`, `ADMIN_EMAIL` (git-ignored)

## Verification

- [ ] `./mvnw compile` clean
- [ ] `./mvnw test` green
- [ ] Register endpoint returns 202 and sends two emails
- [ ] Approve URL creates Firebase user and sends email with reset link
- [ ] Reject URL marks RECHAZADO and sends email
- [ ] Second approve/reject click returns 400
- [ ] Duplicate pending returns 409
- [ ] Re-register within cooldown returns 429
- [ ] Re-register after cooldown succeeds
