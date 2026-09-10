# document-service

Spring Boot 2.7 / Java 11 web service for document CRUD, backed by PostgreSQL
(metadata), S3 (payload) and ActiveMQ (lifecycle events). Uses the **AWS Java
SDK v1**.

## Layout

```
connector/    REST controllers (HTTP surface)
service/      DocumentService (orchestration) + StorageService (S3)
repository/   Spring Data JPA repository (PostgreSQL)
event/        ActiveMQ producer + DocumentEvent
domain/       JPA entity
config/       S3 client + JMS converter + @ConfigurationProperties
```

## Build

Java 11 is required. `gradle.properties` points `org.gradle.java.home` at a
local Temurin 11 install; adjust or remove it if your `JAVA_HOME` is already 11.

```
./gradlew clean build          # compiles; runs the API test (needs Docker)
./gradlew compileJava compileTestJava   # compile only, no Docker
```

## Run locally

```
docker compose up -d                       # postgres + activemq
export APP_S3_ENDPOINT=http://localhost:4566   # e.g. LocalStack, if you want real S3 skip this
export APP_S3_ACCESS_KEY=test APP_S3_SECRET_KEY=test
./gradlew bootRun
```

Without `APP_S3_ENDPOINT` the app talks to real AWS S3 using the default
credential chain and `APP_S3_REGION`.

ActiveMQ web console: http://localhost:8161 (admin/admin).

## API

| Method | Path                        | Body                                   | Result |
|--------|-----------------------------|----------------------------------------|--------|
| POST   | `/api/documents`            | multipart: `file`, `name?`, `description?` | 201 + metadata, `Location` header |
| GET    | `/api/documents`            | –                                      | 200 + list |
| GET    | `/api/documents/{id}`       | –                                      | 200 + metadata / 404 |
| GET    | `/api/documents/{id}/content` | –                                    | 200 + file bytes |
| PUT    | `/api/documents/{id}`       | multipart: `name?`, `description?`, `file?` | 200 + metadata / 404 |
| DELETE | `/api/documents/{id}`       | –                                      | 204 / 404 |

Every create/update/delete publishes a JSON `DocumentEvent` to the
`document-events` queue.

### curl

```
# create
curl -sS -F file=@./notes.txt -F name="Notes" -F description="scratch" \
  http://localhost:8080/api/documents

# list / get / download
curl -sS http://localhost:8080/api/documents
curl -sS http://localhost:8080/api/documents/<id>
curl -sS -OJ http://localhost:8080/api/documents/<id>/content

# update
curl -sS -X PUT -F name="Notes v2" -F file=@./notes-v2.txt \
  http://localhost:8080/api/documents/<id>

# delete
curl -sS -X DELETE -i http://localhost:8080/api/documents/<id>
```

## Testing

`src/test/java/.../DocumentApiIT.java` is the only test — no unit tests. It
drives the full CRUD lifecycle over HTTP with `TestRestTemplate`, using:

- **Docker** (`docker-compose.yml`) for PostgreSQL and ActiveMQ, started by
  Testcontainers' `DockerComposeContainer`.
- **Testcontainers LocalStack** as the S3 mock.

```
./gradlew test
```

Requires a working Docker daemon reachable by the current user.
