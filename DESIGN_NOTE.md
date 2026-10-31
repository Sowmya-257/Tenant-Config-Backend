# MoveInSync Hierarchical Tenant Configuration Service: System Design Note

## 1. System Architecture & High-Level Design

The MoveInSync Configuration System separates configuration management from fast runtime execution. Around 40 microservices read configuration rules (booking cut-off times, night-safety hours, escort requirements, allowed cab types) on every single trip booking and dispatch request. 

Instead of making slow remote HTTP calls over the network, each microservice embeds the `config-client-sdk`.

---

## 2. Consistency Model 
> **MoveInSync Configuration Consistency Model:**  
> The system guarantees **Direct In-Memory Reads with a Maximum 5-Second Sync Delay and Forward-Only Versions**. Local reads are served immediately from an in-memory snapshot (`AtomicReference`), ensuring microservices read settings in less than **0.1 microseconds** without any remote network call. Changes committed to PostgreSQL are saved in atomic transactions with strictly increasing version numbers (1, 2, 3...). Live updates reach microservices within **50 milliseconds** via Server-Sent Events (SSE). If the network drops, a background poller checks the server every 3 seconds, ensuring data is never out-of-date for more than **5 seconds**. Stale or duplicate version packets arriving out of order are dropped automatically.

---

## 3. Sequence Diagram: Atomic Multi-Key Updates & Rollout

The hardest flow is updating multiple related settings at the exact same time (for example: changing night safety start time to `22:00` and end time to `05:00` together) without any microservice reading a mismatched or half-updated state:


---

## 4. Version Monotonicity: Why Versions Only Move Forward

### Question 1: What does a client do when it receives Version 41 after Version 43?

- **Drop and Log:** If a client microservice is already running on Version 43 and receives Version 41, it **drops the update immediately**. The client increments the metric counter `config_rejections_total` and logs a warning: `Rejected stale delta v41 (current is v43)`.
- **Why This Protects the System:** In distributed networks, packets can get delayed or resent. Checking that incoming versions are strictly newer prevents an old delayed packet from overwriting fresher data.

### Question 2: Why do rollbacks create a new version instead of decrementing?

- When an admin clicks "Rollback to Version 41", the server **does not decrement the version number back to 41**. If it did, client services would reject it because they already have Version 43!
- Instead, the server copies the settings from Version 41 and commits a **brand-new forward version**:

$$\text{New Version} = \text{Current Version (43)} + 1 = 44$$

- Because `44 > 43`, every client microservice accepts the update cleanly and applies the rollback without any conflict.

---

## 5. Cost Estimation – Time and Space Complexity Analysis

### Time Complexity (Operation Speed)

| Operation | Complexity | Description |
| :--- | :--- | :--- |
| **Local Config Read (`get()`)** | **O(1)** | Hash table lookup in memory checking at most 4 levels (`SHIFT` -> `OFFICE` -> `CLIENT` -> `GLOBAL`). Latency: **< 0.1 µs (microseconds)**. |
| **Atomic Change Set Publish** | **O(M)** | Where `M` is the count of settings updated. Validates and saves `M` items in a single PostgreSQL database transaction. |
| **Hierarchical Explain Mode** | **O(1)** | Checks at most 4 keys in memory to show which level won and why. |
| **Delta Sync (`sinceVersion`)** | **O(ΔV)** | Where `ΔV` is the count of versions missed while offline. Uses a database index on `(tenant_id, version)` to fetch changes quickly. |

### Space Complexity (Memory & Storage)

| Component | Complexity | Practical Size |
| :--- | :--- | :--- |
| **Client Memory (RAM)** | **O(U)** | Where `U` is total unique settings. For 500 settings x 10 offices x 3 shifts ≈ 15,000 values ≈ **3 to 5 MB RAM** per service. Very lightweight. |
| **Server Database Storage** | **O(T x U + H)** | Where `T` is tenants count, `U` active overrides, and `H` change history. For 1,000 tenants with 50,000 changes/year ≈ **150 MB**. Negligible footprint. |

### Scaling Bottleneck Analysis: Where Does the System Stop Scaling and Why?

#### 1. Open Push Connections Limit (Server Side)
- **Limiting Factor:** Each connected microservice keeps one open socket for Server-Sent Events (SSE). A single standard Spring Boot instance can easily handle up to 10,000 open connections.
- **Why We Are Safe:** We have 40 services running 5 instances each = only 200 total connections. That uses less than 2% of the server's capacity.

#### 2. Database Version Locking During Updates
- **Limiting Factor:** When an admin saves changes, the database briefly locks that tenant's version row to increment the version number safely.
- **Why We Are Safe:** This supports up to 3,500 updates per second per tenant. Since configuration changes are made by human administrators a few times per hour, we are operating well below 1% of capacity.

---

## 6. Failure-Mode Table (Fault Tolerance & Self-Healing)

| Scenario | Failure Condition | What the User / Reader Sees | How the System Recovers |
| :--- | :--- | :--- | :--- |
| **Config Server Crash** | Server process stops or PostgreSQL is down. | Zero service disruption. Trip booking continues reading instantly from RAM. | Client SDK catches the disconnect, keeps serving from memory, and retries connecting in the background with randomized backoff. |
| **Service Restart During Server Outage** | A microservice pod restarts while the config server is dead. | The service boots up successfully without crashing; reads work offline. | `DiskSnapshotStore` loads the last saved configuration from the local disk (`~/.moveinsync/tenant_X.json`). |
| **Network Partition / SSE Push Drop** | Silent socket disconnect between server and client. | Zero errors. Reads remain instant. Data is at most 5 seconds delayed. | `PeriodicReconciliationPoller` polls the server every 3s. It notices the remote version is higher, downloads the missed changes, and updates RAM. |
| **Reconnect Storm** | 500 services reconnect at the exact same second after a server restart. | Server CPU and database remain completely healthy. | Client SDK uses **randomized retry wait times** (jitter) so clients do not hit the server at the exact same millisecond. They also ask only for missed deltas instead of full downloads. |
| **Invalid Setting Value Submitted** | Admin types a negative cut-off time (`-30m`) or invalid time format. | Admin receives an HTTP 400 Bad Request with a clear error message. | Input is rejected at the validation layer before starting the database transaction. Database and clients remain untouched. |
| **Duplicate / Out-of-Order Packet** | Network re-delivery sends Version 41 after Version 43. | No configuration corruption. | Client's `SnapshotMemoryStore` checks `Vincoming <= Vcurrent`, rejects the old payload, and records `config_rejections_total`. |

---

## 7. System Monitoring & 3 AM On-Call Counters

Exposed under `/actuator/metrics` and `/actuator/prometheus` for health dashboards:

- **`config_sync_lag_ms`**: Time in milliseconds between saving a change on the server and receipt by the client (alerts if > 5000 ms).
- **`config_rejections_total`**: Count of stale or duplicate version packets safely rejected.
- **`config_reconnects_total`**: Number of times clients reconnected after a network interruption.
- **`config_live_subscribers_active`**: Number of microservice instances currently listening for live push updates.
- **`config_changes_published_total`**: Total count of atomic configuration change sets committed.
- **`config_rollbacks_total`**: Total count of instant rollbacks executed.

---

## 8. Trade-offs in the System Design

### 1. In-Memory Local Caching vs Remote Network Calls (Redis / HTTP)
- High-throughput booking and routing services cannot wait 5ms to 10ms on every single request. Storing configurations directly in the service's JVM memory gives **< 0.1 µs reads** with zero network bottlenecks.

### 2. Delta Updates with Snapshot Fallback
- Sending only the changed keys (deltas) saves network bandwidth. If a client was offline for days and missed too many versions, the server automatically falls back to sending a complete snapshot.

### 3. Deliberately Left Out (No ZooKeeper or Consul)
- Complex distributed coordination systems (like ZooKeeper or Consul) require heavy operational maintenance and extra server clusters. A simple Spring Boot service with PostgreSQL and Server-Sent Events (SSE) achieves the same real-time performance with minimal operational complexity.
