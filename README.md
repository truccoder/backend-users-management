# backend-users

The **backend-users** service is the central identity and social-graph authority of the platform. It handles everything from authentication and session management to friend requests and user blocking. Other services trust this service as the source of truth for user identity, and they verify tokens using public keys this service exposes.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        backend-users                            │
│                                                                 │
│  REST API                                                       │
│       │                                                         │
│       ▼                                                         │
│  Service Layer  ──── backend-core (shared lib)                  │
│       │                    └── CacheAside<K,V>                  │
│       │                         ├── Redis (L1)                  │
│       │                         └── PostgreSQL (truth)          │
│       │                                                         │
│       ├──── PostgreSQL (writes + authoritative reads)           │
│       └──── Neo4j       (reads only — social graph queries)     │
└─────────────────────────────────────────────────────────────────┘

CDC Pipeline (separate concern):
  PostgreSQL  ──► Debezium ──►  Kafka  ──►  backend-graph-projector  ──►  Neo4j
```

The service never writes directly to Neo4j. Writes go to PostgreSQL atomically, and a Change Data Capture (CDC) pipeline propagates those changes to Neo4j via `backend-graph-projector`. This is a deliberate choice to preserve atomicity: a single PostgreSQL transaction can update both the relational row and emit a reliable, ordered event; trying to write to two different databases in one business operation opens the door to partial failure and split-brain state.

---

## Why PostgreSQL?

**Why not a document store (MongoDB, DynamoDB)?** User identity data is highly relational: users have sessions, sessions belong to users, friend requests link two users, tokens reference users. A document store would force you to either embed and denormalize aggressively (leading to update anomalies). Relational integrity — foreign keys, cascade deletes, unique constraints — is genuinely valuable here and PostgreSQL enforces it at the database level.

---

## Database Schema

PostgreSQL owns four tables.

### `user`

Stores core identity and profile information for each registered user.

### `refreshToken`

Tracks every issued refresh token, enabling per-device session listing and revocation.

### `passwordResetToken`

Short-lived single-use tokens for the forgot-password flow.

### `friendRequest`

Models the lifecycle of a friend request between two users.

---

## Neo4j — Read-Only Graph

Neo4j holds the social graph as projected from PostgreSQL via CDC. This service **only reads** from Neo4j; it never writes to it directly.

The graph contains the following relationship types:

| Relationship | Direction | Meaning |
|---|---|--|
| `FOLLOWS` | `(a)-[:FOLLOWS]->(b)` |
| `FOLLOWED` | mirror of FOLLOWS |
| `BLOCKS` | `(a)-[:BLOCKS]->(b)` |

---

## Caching Strategy

All cache logic lives in **`backend-core`**, the shared library, so every service gets the same battle-tested behaviour without copy-pasting.

### Cache-Aside Pattern

The service never writes to the cache directly on a miss. Instead, it follows a strict read-through discipline:

1. Check Redis first.
2. On a hit, return the cached value.
3. On a miss, acquire a lock, load from PostgreSQL, populate the cache, release the lock.
4. Cache operations are opportunistic. PostgreSQL is the truth.

```
Request
   │
   ▼
Redis GET ──► HIT ──────────────────────────────► Return value
   │                                                  │
   └──► MISS                              (maybe trigger background refresh)
           │
           ▼
       Redis SETNX "lock:{key}"
           │
     ┌─────┴──────┐
   GOT LOCK    LOST LOCK
     │              │
     ▼              ▼
  DB query      wait 100ms → Redis GET → HIT ? return : DB fallback
     │
     ▼
  Redis SET (with jittered TTL)
     │
     ▼
  Return value
```

### TTL Jitter (±15%)

Every `put()` call randomizes the TTL within ±15% of the configured base TTL:

**Why does this matter?** Imagine 10,000 user profile objects all cached at 09:00 with a 5-minute TTL. Without jitter, they all expire simultaneously at 09:05, causing a "thundering herd" — every concurrent request hits PostgreSQL at the same instant. With ±15% jitter, expiries are spread across a 90-second window, smoothing the load curve dramatically.

### Probabilistic Early Refresh

When a cache hit occurs within the last 10% of the key's remaining TTL, a linearly increasing probability triggers a **fire-and-forget background refresh** before the key expires naturally:

This means popular keys are almost always refreshed before they expire, eliminating the latency spike that would otherwise occur on the cold miss.

### Fire-and-Forget with `doOnSuccess`

Cache writes in service code use `doOnSuccess` (reactive), ensuring the cache population happens **after** the database operation succeeds and **does not block** the response path:

If the cache write fails, the user still gets their response. This is a deliberate design: the cache is an optimization, not a dependency.