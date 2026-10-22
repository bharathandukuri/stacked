"""NoSQL and Distributed Caching MCQs."""

QUESTIONS = [
    {
        "title": "CAP Theorem: Network Partition Trade-Offs",
        "slug": "cap-theorem-network-partition-tradeoffs",
        "content": """According to Brewer's **CAP Theorem**, when a network partition ($P$) occurs between nodes in a distributed data store:

Why is a system forced to choose between **Consistency (Linearizability)** and **Availability**?

> [!IMPORTANT]
> A network partition is an unavoidable physical reality in distributed networks.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["NoSQL & Distributed Caching", "Software Architecture & LLD"],
        "tags": ["cap-theorem", "distributed-systems", "consistency", "availability"],
        "options": [
            {
                "id": "opt_a",
                "text": "To guarantee Consistency, isolated partitions must reject writes to prevent stale or conflicting state (sacrificing Availability); to guarantee Availability, isolated nodes must accept writes, causing divergent states (sacrificing Consistency)",
                "explanation": "If nodes cannot communicate due to partition P, they cannot reach consensus on new updates without either blocking (loss of A) or accepting diverging writes (loss of C)."
            },
            {
                "id": "opt_b",
                "text": "Systems can easily achieve Consistency and Availability simultaneously by using faster optical networking hardware",
                "explanation": "Hardware cannot eliminate network partitions or hardware failures."
            },
            {
                "id": "opt_c",
                "text": "Network partitions only affect NoSQL databases; relational SQL databases are immune to CAP theorem constraints",
                "explanation": "Any distributed system (including distributed SQL like CockroachDB or Spanner) is governed by CAP."
            },
            {
                "id": "opt_d",
                "text": "Availability means 100% uptime over a calendar year, while Consistency means foreign key integrity",
                "explanation": "These are colloquial definitions, not CAP formal definitions (A = every non-failing node returns non-error response; C = linearizable read of latest write)."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "When a network partition ($P$) occurs, communication between replicas is severed. If the system accepts writes on an isolated node, other nodes become stale (loss of Consistency). If it rejects writes until network heals, it fails to respond to clients (loss of Availability). Hence: $CP$ or $AP$.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Redis Data Structure Selection",
        "slug": "redis-data-structure-selection",
        "content": """Match each business requirement to the most optimal **native Redis data structure**:

| Requirement | Optimal Data Structure |
| :--- | :--- |
| Real-time gaming leaderboard sorted by score with rank lookups | ? |
| Unique active visitor counter for millions of users with $\\le 1\\%$ error | ? |
| User session object with 15 independent fields updated individually | ? |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["NoSQL & Distributed Caching"],
        "tags": ["redis", "data-structures", "caching", "sorted-set", "hyperloglog"],
        "options": [
            {
                "id": "opt_1",
                "text": "Leaderboard: **Sorted Set (`ZSET`)** | Unique visitor count: **HyperLogLog** | User session: **Hash (`HSET`/`HGET`)**",
                "explanation": "Sorted Sets allow O(log N) score insertion and rank retrieval (ZRANK). HyperLogLog uses fixed 12KB memory to estimate cardinality up to billions with ~0.81% error. Hashes allow setting/getting individual fields without serializing the whole JSON string."
            },
            {
                "id": "opt_2",
                "text": "Leaderboard: **List (`LPUSH`)** | Unique visitor count: **Set (`SADD`)** | User session: **String (`SET`)**",
                "explanation": "Lists require O(N) sorting for leaderboards; Sets consume huge memory for millions of users."
            },
            {
                "id": "opt_3",
                "text": "Leaderboard: **Sorted Set (`ZSET`)** | Unique visitor count: **Bitmap** | User session: **Stream**",
                "explanation": "Streams are append-only message logs, not suitable for single user profile fields."
            },
            {
                "id": "opt_4",
                "text": "Leaderboard: **Hash** | Unique visitor count: **Sorted Set** | User session: **List**",
                "explanation": "Hashes do not provide sorted ordering or ranking."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "Redis `ZSET` (skip list + hash table) provides $\\mathcal{O}(\\log N)$ operations for scores and rankings. `HyperLogLog` counts unique items with fixed $\\approx 12$ KB memory. Redis `Hash` allows granular field updates (`HSET user:1 email \"...\"`) without rewriting whole blobs.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Cache Stampede (Thundering Herd) Mitigation",
        "slug": "cache-stampede-mitigation",
        "content": """A high-traffic e-commerce homepage key expires in Redis at peak load (100,000 requests/sec). All 100,000 concurrent threads experience a cache miss simultaneously and hammer the primary PostgreSQL database to recompute the data, crashing the database.

Which techniques effectively mitigate this **Cache Stampede (Thundering Herd)**?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["NoSQL & Distributed Caching", "Backend Engineering & Spring"],
        "tags": ["caching", "cache-stampede", "thundering-herd", "redis"],
        "options": [
            {
                "id": "opt_lock",
                "text": "**Distributed Mutex Lock (Singleflight)**: Only the first thread that acquires a lock queries the database and warms the cache; all other threads await the result or return stale data",
                "explanation": "Prevents redundant simultaneous DB queries."
            },
            {
                "id": "opt_early_exp",
                "text": "**Probabilistic Early Expiration (XFetch algorithm)**: Background threads probabilistically recompute and refresh the cache item *before* it formally expires",
                "explanation": "XFetch recalculates items early based on compute time and read frequency."
            },
            {
                "id": "opt_jitter",
                "text": "**TTL Jitter**: Adding random variance (e.g. `base_ttl + random(0, 60)`) to expiration times so multiple keys do not expire simultaneously",
                "explanation": "Spreads out expirations across a time window."
            },
            {
                "id": "opt_no_ttl",
                "text": "Completely disabling Redis TTL and relying on manual server restarts to clear memory",
                "explanation": "Disabling TTL causes memory exhaustion (OOM) and stale data."
            }
        ],
        "correctOptionIds": ["opt_lock", "opt_early_exp", "opt_jitter"],
        "explanation": "Cache stampedes are solved by singleflight mutexes (allowing only 1 loader), probabilistic early recomputation (XFetch), and TTL jitter (preventing mass simultaneous expirations).",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Redis Single-Threaded Event Loop Atomicity",
        "slug": "redis-single-threaded-event-loop-atomicity",
        "content": """Because the core Redis command processing engine executes commands on a **single execution thread** using non-blocking I/O multiplexing:

Every individual primitive Redis command (e.g., `INCR`, `SETNX`, `LPUSH`) is inherently atomic and immune to race conditions between commands.

Is this statement **True** or **False**?

> [!NOTE]
> Consider a single Redis standalone instance executing individual commands.""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["NoSQL & Distributed Caching"],
        "tags": ["redis", "atomicity", "concurrency", "event-loop"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "Single-threaded command execution means no two commands execute in parallel on the same server, ensuring atomicity of individual operations."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "Commands are serialized on the event loop."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. The core command execution loop of Redis is single-threaded. Each command runs to completion before the next command is processed, ensuring individual commands like `INCR` or `HINCRBY` are completely atomic without application-level locks.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "MongoDB Document Modeling: Unbounded Arrays Anti-Pattern",
        "slug": "mongodb-unbounded-arrays-anti-pattern",
        "content": """In MongoDB schema design:

Why is embedding an **unbounded array** (e.g. embedding all user comments directly inside a `BlogPost` document) considered a severe anti-pattern?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["NoSQL & Distributed Caching"],
        "tags": ["mongodb", "schema-design", "anti-pattern", "nosql"],
        "options": [
            {
                "id": "opt_a",
                "text": "MongoDB has a hard **16 MB maximum document size limit** (`BSONObj size`); popular posts with thousands of comments will exceed this limit and fail with write errors",
                "explanation": "Single BSON documents cannot exceed 16MB."
            },
            {
                "id": "opt_b",
                "text": "Document growth causes frequent in-place document reallocation and WiredTiger storage engine write amplification",
                "explanation": "Continuously appending to an array forces reallocating memory and rewrites."
            },
            {
                "id": "opt_c",
                "text": "Fetching the blog post metadata forces loading thousands of embedded comments into memory, wasting network bandwidth and RAM",
                "explanation": "Document-level queries load the entire document unless using strict projections."
            },
            {
                "id": "opt_d",
                "text": "MongoDB completely forbids indexing any fields inside embedded arrays",
                "explanation": "False. MongoDB fully supports multikey indexes on array fields."
            }
        ],
        "correctOptionIds": ["opt_a", "opt_b", "opt_c"],
        "explanation": "Embedding unbounded arrays risks hitting the 16MB BSON document limit, degrades query performance by fetching unnecessary data, and causes storage engine write amplification. The idiomatic pattern is subset embedding (e.g., embedding only the top 10 most recent comments) and referencing the rest in a separate collection.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Sliding Window Rate Limiter with Redis Sorted Sets",
        "slug": "sliding-window-rate-limiter-redis-zset",
        "content": """To implement an exact **sliding window rate limiter** in Redis (e.g. max 100 requests per 60 seconds per IP):

We can use a **Redis Sorted Set (`ZSET`)** where both the score and the member identify each request's millisecond timestamp.

What is the correct sequence of Redis commands executed atomically in a transaction or Lua script?

1. Remove elements outside the window: `ZREMRANGEBYSCORE key 0 (now - 60000)`
2. Count current elements in window: `ZCARD key`
3. If count < 100: `ZADD key now requestId` and `EXPIRE key 60`

Is this approach valid for an accurate sliding window rate limiter?
""",
        "type": "BOOLEAN",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["NoSQL & Distributed Caching", "Backend Engineering & Spring"],
        "tags": ["redis", "rate-limiting", "zset", "algorithms"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "ZREMRANGEBYSCORE purges requests older than the sliding threshold (now - windowMs), ZCARD checks remaining requests, and ZADD records the new request, providing a precise sliding window."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "This algorithm is one of the industry-standard implementations of sliding window rate limiting."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. The ZSET sliding window algorithm purges timestamps older than `now - window`, counts remaining elements via `ZCARD`, and admits the request if within the limit. Wrapping the operations in a Lua script or Redis `MULTI`/`EXEC` ensures atomic execution.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Redis Eviction Policies on Out-Of-Memory",
        "slug": "redis-eviction-policies-on-oom",
        "content": """When Redis memory usage exceeds `maxmemory`, what is the difference between the **`allkeys-lru`** and **`volatile-lru`** eviction policies?

| Policy | Eligible Keys for Eviction |
| :--- | :--- |
| `allkeys-lru` | ? |
| `volatile-lru` | ? |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["NoSQL & Distributed Caching"],
        "tags": ["redis", "eviction", "lru", "caching", "memory-management"],
        "options": [
            {
                "id": "opt_a",
                "text": "**allkeys-lru** evicts the least recently used keys out of the *entire keyspace*; **volatile-lru** evicts least recently used keys only among keys that have an *explicit expiration (TTL)* set",
                "explanation": "If a key has no TTL set, volatile-lru will never evict it, potentially throwing OOM errors if all keys lack TTLs. allkeys-lru treats any key as eligible for LRU eviction."
            },
            {
                "id": "opt_b",
                "text": "allkeys-lru deletes all keys immediately, while volatile-lru only deletes volatile memory caches",
                "explanation": "allkeys-lru does not flush all keys at once; it evicts keys progressively as needed."
            },
            {
                "id": "opt_c",
                "text": "volatile-lru writes expired keys to disk, while allkeys-lru writes to SSD",
                "explanation": "Redis eviction policies purge keys from RAM, not write them to disk."
            },
            {
                "id": "opt_d",
                "text": "allkeys-lru only works with Redis Cluster, while volatile-lru works only with standalone Redis",
                "explanation": "Both policies are available in all Redis deployments."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "`allkeys-lru` is the standard choice for general-purpose caching where any key can be evicted when memory is full. `volatile-lru` preserves keys without TTLs, only evicting keys that have an explicit expiration timestamp configured.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "MongoDB Write Concern: majority and Journaling",
        "slug": "mongodb-write-concern-majority-journaling",
        "content": """In a MongoDB replica set deployment:

Setting Write Concern to `{ w: "majority", j: true }` guarantees that a write acknowledgment is returned to the client **only after** the write operation has been written to the on-disk journal on a majority of voting replica set members.

This configuration provides the highest durability against primary node crashes and rollback of committed writes.

Is this statement **True** or **False**?
""",
        "type": "BOOLEAN",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["NoSQL & Distributed Caching"],
        "tags": ["mongodb", "write-concern", "durability", "replication"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "`w: majority` ensures a majority of replica nodes acknowledge the write, and `j: true` ensures the write is persisted to the on-disk write-ahead journal before acknowledgment."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "This is the exact definition of majority write concern with on-disk journaling."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. In MongoDB, `{ w: 'majority', j: true }` guarantees that the write is recorded in the on-disk journal across a strict majority of replica set nodes before the driver receives an acknowledgment, preventing data loss or rollbacks during elections.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    }
]
