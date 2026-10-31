# MoveInSync Hierarchical Tenant Configuration Service

> **Hierarchical Tenant Configuration with Live Propagation**

MoveInSync runs hundreds of corporate clients on one platform, with each client tuning hundreds of settings: booking cut-off time, maximum ride time, night-safety window, escort rules, and allowed cab types. Settings are defined at four distinct levels: **`GLOBAL` $\to$ `CLIENT` $\to$ `OFFICE` $\to$ `SHIFT`**, with the **most specific value winning**.

---

## Prerequisites

- **Java 21** or higher
- **PostgreSQL 16** (or **Docker** installed)

---

## Option 1: Run Locally with Maven

### 1. Database Setup
Ensure PostgreSQL is running on port `5432` with a database named `moveinsync_config`:
```sql
CREATE DATABASE moveinsync_config;
```

### 2. Start the Backend Server

**Windows (PowerShell / Command Prompt):**
```powershell
.\mvnw.cmd spring-boot:run -pl config-server
```

**macOS / Linux:**
```bash
./mvnw spring-boot:run -pl config-server
```

The server will start on **`http://localhost:8090`** and auto-initialize all database schemas.

---

## Option 2: Run with Docker

### 1. Start PostgreSQL Container
If you don't have PostgreSQL installed locally, launch it using Docker:
```bash
docker run -d \
  --name moveinsync-postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=moveinsync_config \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:16-alpine
```

### 2. Build the Docker Image
From the root of the repository, build the multi-stage image:
```bash
docker build -t moveinsync-config-server .
```

### 3. Run the Backend Container
Run the container and connect it to your database:

**Windows / macOS:**
```bash
docker run -d -p 8090:8090 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/moveinsync_config" \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  --name config-server \
  moveinsync-config-server
```

**Linux:**
```bash
docker run -d -p 8090:8090 \
  --add-host=host.docker.internal:host-gateway \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/moveinsync_config" \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  --name config-server \
  moveinsync-config-server
```

Check logs to verify startup:
```bash
docker logs -f config-server
```

---

## Running the Automated Test Suite

Run all unit tests, concurrency tests, and benchmark suites using the included Maven wrapper:

**Windows:**
```powershell
.\mvnw.cmd test
```

**macOS / Linux:**
```bash
./mvnw test
```

---

## Frontend Web Console

The React + Vite frontend dashboard is maintained in its dedicated repository:  
👉 [Tenant-Config-Frontend](https://github.com/Sowmya-257/Tenant-Config-Frontend)

---

## Design and Evaluation Criteria Reference

Please refer to [DESIGN_NOTE.md](./DESIGN_NOTE.md) for:
- Complete Architecture Diagram
- Sequence Diagram for Atomic Multi-Key Updates
- Failure-Mode Table & Self-Healing Guarantees
- Consistency Model & Bounded Staleness Specification
- Time & Space Complexity Analysis
