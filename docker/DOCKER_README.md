# 🐳 Docker Setup – Spring Boot + PostgreSQL + MinIO

This folder contains all **Docker Compose** configurations for running your Spring Boot application with
**PostgreSQL database** and **MinIO object storage** in different modes:

- **EMPTY** – creates **schema only** (no data)
- **SEED** – creates **schema + loads sample data**
- **PERSIST** – keeps existing data (no reinitialization)
- **MINIO** – adds local S3-compatible MinIO storage

> Note: PostgreSQL initialization scripts inside `/docker-entrypoint-initdb.d` run **only the first time**
> a new data volume is created.

---

## 🔁 Reset Database (re-run init scripts)

Initialization scripts run **only on fresh volumes**.  
To recreate tables and reload seed data, reset the volume:

```bash
docker compose -f compose.pg-seed.yml down -v
docker compose -f compose.pg-seed.yml up -d
```

`-v` removes the old data volume, forcing PostgreSQL to re-run your init scripts.

---

## 📂 Folder Structure

```
docker/
├─ .env                      # shared environment variables for all compose files
├─ compose.app.yml           # Spring Boot application
├─ compose.pg-empty.yml      # PostgreSQL – schema only
├─ compose.pg-seed.yml       # PostgreSQL – schema + sample data
├─ compose.pg-persist.yml    # PostgreSQL – persistent data
├─ compose.minio.yml         # MinIO – local S3-compatible storage
└─ postgres/
   ├─ init-schema/
   │  └─ 01_schema.sql
   └─ init-seed/
      └─ 02_seed.sql
```

---

## ⚙️ `.env` file

Shared environment file used by all compose configurations:

```
APP_PORT=8080

DB_HOST=db
DB_PORT=5432
DB_NAME=appdb
DB_USER=app
DB_PASSWORD=app

# MinIO (S3-compatible storage)
MINIO_ROOT_USER=minio
MINIO_ROOT_PASSWORD=minio-secret
MINIO_BUCKET=files
MINIO_PORT_API=9000
MINIO_PORT_CONSOLE=9001
```

> ⚠️ **Warning:** This file contains plaintext credentials intended **only for local development**.  
> Never commit real passwords, secrets, or production credentials to version control.  
> For shared repositories, create a safe `.env.example` without sensitive values and add the actual `.env` file to
`.gitignore`.

---

## 🚀 Running Containers

All commands are executed from the **`docker/`** directory.

### 🧱 Run only the database

- **EMPTY (schema only)**
  ```bash
  docker compose -f compose.pg-empty.yml up -d
  ```

- **SEED (schema + sample data)**
  ```bash
  docker compose -f compose.pg-seed.yml up -d
  ```

- **PERSIST (keep data)**
  ```bash
  docker compose -f compose.pg-persist.yml up -d
  ```

### 🧩 Run MinIO (object storage)

Start MinIO only:

```bash
docker compose -f compose.minio.yml up -d
```

MinIO Web Console: [http://localhost:9001](http://localhost:9001)  
Login with `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`.

Default S3 API endpoint: `http://localhost:9000`

---

### ⚡ Run application + database + MinIO together

Combine all compose files as needed:

```bash
docker compose -f compose.app.yml -f compose.pg-persist.yml -f compose.minio.yml up --build
```

or replace `persist` with `seed` / `empty` depending on your needs.

Your Spring Boot app can then access both Postgres and MinIO inside the shared Docker network.

---

## 🧠 Connect from IntelliJ IDEA

| Setting      | Value       |
|--------------|-------------|
| **Host**     | `localhost` |
| **Port**     | `5432`      |
| **Database** | `appdb`     |
| **User**     | `app`       |
| **Password** | `app`       |

Steps:  
**View → Tool Windows → Database → + → Data Source → PostgreSQL → Fill the fields → Test Connection**

---

## 🧰 Useful Commands

| Action                            | Command                                            |
|-----------------------------------|----------------------------------------------------|
| List running containers           | `docker ps`                                        |
| Show PostgreSQL logs              | `docker logs -f db`                                |
| Open psql inside container        | `docker exec -it db psql -U app -d appdb`          |
| Stop and remove DB including data | `docker compose -f compose.pg-persist.yml down -v` |
| Show MinIO logs                   | `docker logs -f minio`                             |
| Open MinIO client shell           | `docker exec -it minio /bin/sh`                    |

---

## 🕸️ Network

All compose files use the shared network `appnet`:

```yaml
networks:
  appnet:
    name: appnet
    driver: bridge
```

When running app + DB + MinIO together, all services join the same network automatically.
