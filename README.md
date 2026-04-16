# MedSync Backend

Backend de la plataforma MedSync, un sistema de farmacovigilancia que analiza artículos científicos con IA y los cruza con el contexto clínico de pacientes para generar alertas sobre medicamentos obsoletos o nueva evidencia.

## Stack

- **Framework**: Quarkus
- **Lenguaje**: Java 21
- **Base de datos**: MySQL (producción), H2 (desarrollo)
- **ORM**: Hibernate ORM con Panache
- **API**: REST + Jackson + OpenAPI
- **Validación**: Hibernate Validator

## Arquitectura

Este proyecto sigue **Clean Architecture** con organización **Capa > Feature**.

### Las 4 capas

```
src/main/java/itesm/medsync/
│
├── domain/             ← Reglas de negocio puras (cero dependencias externas)
│   └── <feature>/
│       ├── model/         POJOs del negocio
│       ├── usecase/       Interfaces de casos de uso
│       ├── repository/    Interfaces de repositorios y gateways
│       └── exception/     Excepciones del negocio
│
├── application/        ← Implementación de los casos de uso
│   └── <feature>/         Services que implementan las interfaces de domain/usecase
│
├── infrastructure/     ← Implementaciones concretas + configuración
│   ├── persistence/       BD MedSync (MySQL): JPA Entities, Panache, Mappers
│   │   └── <feature>/
│   ├── hospital/          Gateway a la BD externa del hospital
│   ├── ai/                Gateway al servicio de IA
│   └── config/            Exception handlers, CORS, datasources
│
└── interfaces/         ← Puntos de entrada al sistema
    └── rest/
        └── <feature>/     JAX-RS Resources, DTOs, RestMappers
```

### Reglas de dependencia

```
domain/          ← no importa nada externo
application/     ← solo importa domain/
infrastructure/  ← importa domain/ + frameworks (JPA, Panache)
interfaces/      ← importa domain/ + Jakarta REST
```

### Nomenclatura

- **`*Repository`** → para datos propios de MedSync (CRUD)
- **`*Gateway`** → para servicios externos (BD del hospital, IA)
- Ambos viven en `domain/<feature>/repository/` porque son output ports

### Features planeados

- `user` — Usuarios del sistema
- `patient` — Pacientes
- `medication` — Medicamentos
- `article` — Artículos científicos y tags
- `alert` — Alertas generadas

## Cómo correr

### Modo desarrollo (con live reload)

```shell
./mvnw quarkus:dev
```

La Dev UI estará disponible en <http://localhost:8080/q/dev/>.

### Empaquetar

```shell
./mvnw package
```

Produce `target/quarkus-app/quarkus-run.jar`. Para correrlo:

```shell
java -jar target/quarkus-app/quarkus-run.jar
```

### Ejecutable nativo

```shell
./mvnw package -Dnative
```

O sin GraalVM instalado:

```shell
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## Documentación de la API

Una vez corriendo, la documentación OpenAPI/Swagger está en:
- OpenAPI: <http://localhost:8080/q/openapi>
- Swagger UI: <http://localhost:8080/q/swagger-ui>

## Recursos

- [Quarkus](https://quarkus.io/)
- [Hibernate ORM con Panache](https://quarkus.io/guides/hibernate-orm-panache)
- [REST Jackson](https://quarkus.io/guides/rest#json-serialisation)
- [Datasource (MySQL/H2)](https://quarkus.io/guides/datasource)
- [Hibernate Validator](https://quarkus.io/guides/validation)
- [SmallRye OpenAPI](https://quarkus.io/guides/openapi-swaggerui)
