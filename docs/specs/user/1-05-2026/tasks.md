# Feature: User — Implementation Tasks

> Status: **all tasks complete**. Historical record.

## Domain
- [x] `UserTest`, `RoleTest` (constructor invariants, factory) → RED
- [x] `User`, `Role` POJOs
- [x] `UserWithRole` record, `KnownRoles` constants
- [x] Domain exceptions: `UserNotFoundException`, `UserAlreadyExistsException`, `RoleNotFoundException`, `RoleMismatchException`, `InvalidUserDataException`, `InvalidRoleDataException`
- [x] `UserRepository`, `RoleRepository`, `FirebaseUserGateway` interfaces
- [x] `LoginOrRegisterUserUseCase`, `CreateAdminUserUseCase` interfaces

## Application
- [x] `RegisterUserServiceTest` (existing / new user) → RED
- [x] `RegisterUserService`
- [x] `CreateAdminUserServiceTest` (happy path, duplicate, role-not-found, compensation on DB failure) → RED
- [x] `CreateAdminUserService` with Firebase rollback logic

## Persistence
- [x] Migrations: `V2__create_users_schema.sql`, `V3__seed_roles.sql`
- [x] `UserRepositoryImplTest`, `RoleRepositoryImplTest` → RED
- [x] `UserEntity` (`@NamedEntityGraph("User.withRole")`), `RoleEntity`
- [x] `UserPersistenceMapper`, `RolePersistenceMapper`
- [x] `UserRepositoryImpl`, `RoleRepositoryImpl`

## Infrastructure (security)
- [x] `FirebaseInitializer` (`@Startup`) — load credentials from `firebase.config.path`
- [x] `FirebaseAuthFilter` (`@Provider`, AUTHENTICATION priority)
- [x] `FirebaseUserGatewayImpl`
- [x] `AuthenticatedUserContext` (`@RequestScoped`)

## REST
- [x] Integration tests for `UserResource`, `AuthResource`, `AdminResource` → RED
- [x] DTOs: `LoginRequest`, `CreateUserRequest`, `UserResponse`
- [x] `UserRestMapper`
- [x] `UserResource` (`GET /api/users/me`)
- [x] `AuthResource` (`POST /api/auth/login`)
- [x] `AdminResource` (`POST /api/admin/users`)
- [x] Wire all new exceptions into `GlobalExceptionHandler` with correct HTTP codes

## Verification
- [x] `./mvnw test` green (with Firebase mocked / disabled in `%test`)
- [x] Manual smoke test via Swagger UI + a valid Firebase token

## Cross-feature
- [x] `V4__add_patient_medico_fk.sql` — add FK `patients.medico_id → usuarios.id` (deferred from patient V1)
