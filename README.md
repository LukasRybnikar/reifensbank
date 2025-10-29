# Reifensbank DMS – README

A lightweight Document Management System (DMS) built on **Spring Boot 3**, **JWT authentication**, **PostgreSQL**, and *
*MinIO**.

> **Base API URL (local):** `http://localhost:8080/api`  
> **Active Profile:** `localhost`

---

## 🔧 Requirements

- **Java 21** (OpenJDK 21)
- **Maven 3.9+** (included `mvnw` / `mvnw.cmd`)
- **Docker** (optional but recommended for PostgreSQL + MinIO stack)
- If not using Docker:
    - **PostgreSQL 15+**
    - **MinIO** (S3-compatible storage) or disable the integration temporarily

> **Configuration files:**
> - Main config: `src/main/resources/application.yml`
> - Local profile: `src/main/resources/application-localhost.yml`
    >   - sets `server.servlet.context-path: /api` and `server.port: 8080`
    >
- links DB & MinIO via environment variables (`DB_*`, `MINIO_*`, `APP_PORT`, etc.)

---

## 🐳 Docker (brief)

Full Docker setup is located in the **`/docker`** folder – includes multiple Compose variants for DB + MinIO (empty,
seeded, persistent).  
➡️ **See:** [`docker/DOCKER_README.md`](docker/DOCKER_README.md)

---

## ▶️ Build & Run

### 1) Build the application

```bash
# from repository root
./mvnw clean package -DskipTests
# result: target/reifensbank-0.0.1-SNAPSHOT.war
```

### 2) Run with profile `localhost`

**A) Using spring-boot:run**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=localhost
```

**B) Using built WAR**

```bash
java -jar target/reifensbank-0.0.1-SNAPSHOT.war --spring.profiles.active=localhost
```

> If using Docker Compose from `/docker`, start services as described there (default ports assumed).

---

## 🔐 Authentication

All endpoints except **`POST /auth/login`** require a **Bearer JWT** token.

Example login and token export:

```bash
curl -X POST "http://localhost:8080/api/auth/login"   -H "Content-Type: application/json"   -d '{"username": "user", "password": "password"}'

# Save token
TOKEN="YOUR_JWT_TOKEN"
```

---

## 📚 API – Quick Examples

### 1) Auth – Login

`POST /auth/login`

```bash
curl -X POST "http://localhost:8080/api/auth/login"   -H "Content-Type: application/json"   -d '{"username": "user", "password": "password"}'
# → { "accessToken": "...", "tokenType": "Bearer", "expiresIn": 3600 }
```

---

### 2) Documents

#### 2.1 Create a document (metadata + binary upload)

`POST /documents`

```bash
curl -X POST "http://localhost:8080/api/documents"   -H "Authorization: Bearer $TOKEN"   -F "file=@./samples/invoice.pdf"   -F "name=Invoice 2025-01"   -F "type=pdf"
```

#### 2.2 Replace file content

`PUT /documents/{id}/content`

```bash
DOC_ID="11111111-2222-3333-4444-555555555555"

curl -X PUT "http://localhost:8080/api/documents/$DOC_ID/content"   -H "Authorization: Bearer $TOKEN"   -F "file=@./samples/invoice_updated.pdf"
```

#### 2.3 Update metadata

`PATCH /documents/{id}`

```bash
curl -X PATCH "http://localhost:8080/api/documents/$DOC_ID"   -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json"   -d '{"name": "Invoice 2025-01 (final)", "type": "pdf"}'
```

#### 2.4 Delete document

`DELETE /documents/{id}`

```bash
curl -X DELETE "http://localhost:8080/api/documents/$DOC_ID"   -H "Authorization: Bearer $TOKEN"
```

> Note: `GET /documents/{id}/content` (download) and  
> `GET /documents/{id}/content/info` are currently *Not implemented*.

---

### 3) Protocols

#### 3.1 Create a protocol

`POST /protocols`

```bash
curl -X POST "http://localhost:8080/api/protocols"   -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json"   -d '{
        "state": "NEW",
        "documentIds": [
          "11111111-2222-3333-4444-555555555555",
          "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        ]
      }'
```

#### 3.2 Get a protocol by ID

`GET /protocols/{id}`

```bash
PROT_ID="99999999-8888-7777-6666-555555555555"

curl -X GET "http://localhost:8080/api/protocols/$PROT_ID"   -H "Authorization: Bearer $TOKEN"
```

#### 3.3 Update a protocol (full update)

`PUT /protocols/{id}`

```bash
curl -X PUT "http://localhost:8080/api/protocols/$PROT_ID"   -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json"   -d '{
        "state": "PREPARE_FOR_SHIPMENT",
        "documentIds": ["11111111-2222-3333-4444-555555555555"]
      }'
```

#### 3.4 Change protocol state

`PUT /protocols/{id}/state`

```bash
curl -X PUT "http://localhost:8080/api/protocols/$PROT_ID/state"   -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json"   -d '{ "state": "CANCELED" }'
```

> Allowed states: `NEW`, `PREPARE_FOR_SHIPMENT`, `CANCELED`

---

## 🧩 Schemas & OpenAPI

- OpenAPI spec: `src/main/openapi/openapi.yaml`
- Generated models and API interfaces (via `openapi-generator-maven-plugin`)
- Implementations located in `src/main/java/com/task/reifensbank/controller/`

---

## 🛡️ Security

- JWT (HS256) – secret, issuer, and expiration configured in `application-localhost.yml`
- Domain-specific security rules (Auth, Documents, Protocols)

---

## 🗄️ Persistence & S3

- PostgreSQL (configurable via `DB_*` env vars)
- MinIO (S3-compatible; env vars: `MINIO_HOST`, `MINIO_ACCESS_KEY`, etc.)
- Easiest setup via Docker Compose → see [`docker/DOCKER_README.md`](docker/DOCKER_README.md)

---

## 📦 Packaging

- Type: **WAR** (executable Spring Boot WAR)
- Can be run directly with `java -jar`

---

---

## 👥 Default Users (Seeded Docker Environment)

When you run the **seeded Docker** stack, the database is pre-populated with the following users and RBAC setup.

**Shared password for all users:** `password`

### Roles & Authorities (from seed)

- `DOC_READ` → `VIEW_DOCUMENT`
- `DOC_CREATE` → `VIEW_DOCUMENT`, `CREATE_DOCUMENT`
- `DOC_EDIT` → `VIEW_DOCUMENT`, `CREATE_DOCUMENT`, `EDIT_DOCUMENT`
- `PROT_READ` → `VIEW_PROTOCOL`
- `PROT_CREATE` → `VIEW_PROTOCOL`, `CREATE_PROTOCOL`
- `PROT_EDIT` → `VIEW_PROTOCOL`, `CREATE_PROTOCOL`, `EDIT_PROTOCOL`

### Seeded Users

| Username            | Assigned Roles | Effective Authorities     |
|---------------------|----------------|---------------------------|
| Username            | Password       | Assigned Roles            | Effective Authorities |
| ------------------- | -----------    | ------------------------- |-----------------------|
| `reader_doc`        | `password`     | `DOC_READ`                | `VIEW_DOCUMENT` |
| `creator_doc`       | `password`     | `DOC_CREATE`              | `VIEW_DOCUMENT`, `CREATE_DOCUMENT` |
| `editor_doc`        | `password`     | `DOC_EDIT`                | `VIEW_DOCUMENT`, `CREATE_DOCUMENT`, `EDIT_DOCUMENT` |
| `protocol_master`   | `password`     | `PROT_EDIT`               | `VIEW_PROTOCOL`, `CREATE_PROTOCOL`, `EDIT_PROTOCOL` |

---

## 📁 File Size Limitation

The current implementation supports uploads of files **up to 50 MB**.  
For larger files, a different upload approach (e.g., streaming or chunked transfer) would be required.

---

---

## 🧭 TODO

- [ ] Implement remaining OpenAPI endpoints that currently return **501 Not Implemented**
- [ ] Add **malware scanning** for uploaded files (to improve security)
- [ ] Validate and extend **unit tests** (currently auto-generated by AI)
- [ ] Add **Spring profiles** for staging, testing, and production environments