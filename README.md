# MoveInSync Hierarchical Tenant Configuration Service

> **Hierarchical Tenant Configuration with Live Propagation**

MoveInSync runs hundreds of corporate clients on one platform, with each client tuning hundreds of settings: booking cut-off time, maximum ride time, night-safety window, escort rules, and allowed cab types. Settings are defined at four distinct levels: **`GLOBAL` $\to$ `CLIENT` $\to$ `OFFICE` $\to$ `SHIFT`**, with the **most specific value winning**.



## Quick Start (Backend)

Ensure PostgreSQL is running on port `5432` with a database named `moveinsync_config` (or configure connection details via environment variables).

```bash
# Windows
.\mvnw.cmd spring-boot:run -pl config-server

# Linux / macOS
./mvnw spring-boot:run -pl config-server
```
The server will start on port `8090` (or `PORT` if specified) and auto-initialize schemas.

---

## Frontend Web Console
The React + Vite frontend dashboard is maintained in its dedicated repository:
👉 [Tenant-Config-Frontend](https://github.com/Sowmya-257/Tenant-Config-Frontend)

---

## Running the Automated Test Suite

Run all unit tests, concurrency tests, and benchmark suites using the included Maven wrapper:

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```


## Design and Evaluation Criteria Reference

Please refer to [`DESIGN_NOTE.md`](./DESIGN_NOTE.md) for:
1. Complete Architecture Diagram
2. Sequence Diagram for Atomic Multi-Key Updates
3. Failure-Mode Table & Self-Healing Guarantees
4. Consistency Model & Bounded Staleness Specification
5. Time & Space Complexity Analysis
