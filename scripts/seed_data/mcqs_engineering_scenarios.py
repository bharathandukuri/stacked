"""Engineering Scenarios, Debugging, and Communication MCQs."""

QUESTIONS = [
    {
        "title": "Code Review: Concurrent Race Condition in Counter",
        "slug": "code-review-concurrent-counter-race-condition",
        "content": """During a code review of a high-throughput Spring Boot service, you encounter the following class:

```java
@Service
public class MetricsCollector {
    private long requestCount = 0;

    public void recordRequest() {
        requestCount++; // Non-atomic compound operation
    }

    public long getRequestCount() {
        return requestCount;
    }
}
```

What defect does this code contain, and how should it be remediated?

> [!WARNING]
> Singleton Spring services are invoked concurrently by dozens of HTTP worker threads.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Debugging", "Core Java"],
        "tags": ["code-review", "concurrency", "race-condition", "atomics"],
        "options": [
            {
                "id": "opt_a",
                "text": "It exhibits a critical **race condition (data loss)**: `requestCount++` is a read-modify-write composite operation; concurrent threads will overwrite each other's increments. Fix by using `java.util.concurrent.atomic.LongAdder` or `AtomicLong`",
                "explanation": "Because requestCount++ is non-atomic, threads will interleave reads and writes, resulting in lost updates. LongAdder is specifically optimized for high-throughput write-heavy counters."
            },
            {
                "id": "opt_b",
                "text": "Declaring `private volatile long requestCount = 0;` will make `requestCount++` completely thread-safe and atomic",
                "explanation": "Volatile provides visibility, but does NOT make composite read-modify-write operations atomic."
            },
            {
                "id": "opt_c",
                "text": "The code causes a deadlock whenever two threads invoke recordRequest() simultaneously",
                "explanation": "There are no locks present, so deadlock is impossible."
            },
            {
                "id": "opt_d",
                "text": "The code is completely safe because Spring Boot manages thread safety for all services automatically",
                "explanation": "Spring does not synchronize state on user-defined singleton services."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "`requestCount++` compiles to three distinct bytecode instructions: reading the variable, incrementing it, and writing back to memory. Without synchronization, concurrent threads interleave these steps and lose increments. `LongAdder` or `AtomicLong` must be used for lock-free thread safety.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Incident Investigation: Unbounded CachedThreadPool Exhaustion",
        "slug": "incident-unbounded-threadpool-exhaustion",
        "content": """During a sudden Black Friday flash sale traffic spike, an application's servers crash with:

```text
java.lang.OutOfMemoryError: unable to create native thread
```

Upon inspecting the thread dump and source code, you find:

```java
ExecutorService executor = Executors.newCachedThreadPool();
```

What is the root cause of this failure?

> [!CAUTION]
> Consider the default maximum pool size of `newCachedThreadPool()`.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Debugging", "Core Java"],
        "tags": ["incident", "production", "thread-pool", "oom", "debugging"],
        "options": [
            {
                "id": "opt_1",
                "text": "`newCachedThreadPool` uses an **unbounded maximum pool size (`Integer.MAX_VALUE`)** with a `SynchronousQueue`; under high traffic, it creates a new OS thread for every incoming task until host RAM or OS thread limits (`/proc/sys/kernel/threads-max`) are exhausted",
                "explanation": "CachedThreadPool spawns threads without upper bounds. In production, a bounded ThreadPoolExecutor with a fixed capacity and backpressure rejection policy (e.g. CallerRunsPolicy) must always be used."
            },
            {
                "id": "opt_2",
                "text": "CachedThreadPool does not allow tasks to execute if CPU utilization exceeds 80%",
                "explanation": "CachedThreadPool has no CPU throttling mechanism."
            },
            {
                "id": "opt_3",
                "text": "The JVM heap was exhausted by large 100MB byte arrays",
                "explanation": "The error specifically states 'unable to create native thread', which is an OS thread memory limit, not JVM heap space."
            },
            {
                "id": "opt_4",
                "text": "The operating system forcibly terminated the JVM due to network socket timeouts",
                "explanation": "Network timeouts do not cause 'unable to create native thread' OOM errors."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "`Executors.newCachedThreadPool()` has `maximumPoolSize = Integer.MAX_VALUE`. When tasks arrive faster than they can finish, it keeps spawning native OS threads. Each thread requires native memory for its call stack (typically 1MB via `-Xss`), quickly exhausting OS thread limits or virtual memory.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Incident Investigation: Database Connection Pool Exhaustion",
        "slug": "incident-database-connection-pool-exhaustion",
        "content": """An application running on HikariCP connection pool suddenly freezes: all incoming requests hang and eventually fail with `ConnectionTimeoutException: Connection is not available, request timed out after 30000ms`.

Reviewing the logs reveals that the database server itself has 5% CPU usage and plenty of free connections.

Which code pattern is the most probable culprit for this connection leak?

```java
// Pattern A:
Connection conn = dataSource.getConnection();
try {
    process(conn);
} finally {
    conn.close(); // Returns to pool
}

// Pattern B:
Connection conn = dataSource.getConnection();
process(conn); // If process() throws RuntimeException, conn is never closed!
conn.close();
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Debugging", "SQL & Relational Databases"],
        "tags": ["hikaricp", "connection-leak", "incident", "database", "debugging"],
        "options": [
            {
                "id": "opt_b",
                "text": "**Pattern B**: If `process(conn)` throws an unhandled exception, `conn.close()` is never called, leaving the connection permanently checked out until the pool is starved",
                "explanation": "Without try-with-resources or try-finally, any unexpected exception bypasses conn.close(), leaking the connection from the pool."
            },
            {
                "id": "opt_a",
                "text": "**Pattern A**: The finally block causes deadlocks on HikariCP internal locks",
                "explanation": "Pattern A is the classic safe pattern (or try-with-resources)."
            },
            {
                "id": "opt_c",
                "text": "Neither; database connections are automatically reclaimed whenever garbage collection runs",
                "explanation": "Relying on finalizers or GC to close physical sockets causes severe pool exhaustion long before GC runs."
            },
            {
                "id": "opt_d",
                "text": "Connection pools automatically close connections when requests take longer than 1 millisecond",
                "explanation": "Pools do not kill active in-use connections after 1ms."
            }
        ],
        "correctOptionIds": ["opt_b"],
        "explanation": "Failing to close pooled database connections in a `try-with-resources` block or `finally` clause guarantees connection leaks whenever an exception occurs. Once the maximum pool size (e.g. 10 or 20) is leaked, all future callers hang waiting for connections.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "SQL Injection Identification and Remediation",
        "slug": "sql-injection-identification-remediation",
        "content": """Which statements regarding **SQL Injection vulnerabilities** and remediation are correct?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Debugging", "SQL & Relational Databases"],
        "tags": ["security", "sql-injection", "owasp", "prepared-statements"],
        "options": [
            {
                "id": "opt_prep",
                "text": "**Parameterized Prepared Statements**: Transmit query structure and user inputs separately to the database engine, ensuring input data is never interpreted as executable SQL commands",
                "explanation": "Prepared statements completely neutralize SQL injection by compiling the query plan before binding parameters."
            },
            {
                "id": "opt_cat",
                "text": "String concatenation (`\"SELECT * FROM users WHERE user = '\" + input + \"'\"`) is inherently vulnerable to SQL injection",
                "explanation": "Concatenation allows attackers to inject SQL syntax fragments (e.g. ' OR '1'='1)."
            },
            {
                "id": "opt_sanitize",
                "text": "Regular expression blacklisting of characters like `'` or `--` is more secure than using parameterized prepared statements",
                "explanation": "Blacklisting is notoriously brittle and bypassable (e.g. using hex encoding or comments)."
            },
            {
                "id": "opt_orm",
                "text": "Modern ORMs like Hibernate or JPA completely prevent SQL injection, even when developers write raw native string concatenations inside `createNativeQuery(...)`",
                "explanation": "If a developer concatenates raw strings into an ORM query, it is still fully vulnerable."
            }
        ],
        "correctOptionIds": ["opt_prep", "opt_cat"],
        "explanation": "SQL injection occurs when untrusted input is concatenated directly into SQL text. Parameterized queries (`PreparedStatement` or ORM bind parameters) separate the query plan from parameters, neutralizing injection. Blacklisting and blind trust in ORMs without parameterized queries are insecure.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Architectural Trade-Offs: WebSockets vs SSE vs Polling",
        "slug": "architectural-tradeoffs-websockets-sse-polling",
        "content": """An engineering team is designing a **real-time cryptocurrency price ticker** (server pushes updates to web browser every 500ms; client never sends messages back to server).

Which technology provides the best trade-off in terms of protocol simplicity, firewall friendliness, built-in reconnection, and low overhead?

| Technology | Directionality | Protocol | Built-in Auto-Reconnect |
| :--- | :--- | :--- | :--- |
| **Short Polling** | Unidirectional (Client pulls) | HTTP request/response loop | Manual timer |
| **Server-Sent Events (SSE)** | Unidirectional (Server pushes) | Standard HTTP/2 streaming (`text/event-stream`) | Yes (Browser native) |
| **WebSockets** | Full-Duplex Bi-directional | TCP upgrade (`ws://`) | Requires custom heartbeat & reconnect |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Debugging", "Backend Engineering & Spring"],
        "tags": ["trade-offs", "websockets", "sse", "real-time", "architecture"],
        "options": [
            {
                "id": "opt_sse",
                "text": "**Server-Sent Events (SSE)**: It is unidirectional (server-to-client), operates over standard HTTP/2 (bypassing firewall and proxy issues with WebSockets), and browsers natively handle auto-reconnection and event IDs",
                "explanation": "Since the client never sends messages back, full-duplex WebSockets introduce unnecessary protocol overhead, stateful connection handling, and firewall issues compared to lightweight SSE."
            },
            {
                "id": "opt_ws",
                "text": "**WebSockets**: Must always be chosen for any real-time application regardless of whether client-to-server traffic exists",
                "explanation": "WebSockets require protocol upgrades, custom reconnect logic, and separate routing infrastructure."
            },
            {
                "id": "opt_poll",
                "text": "**Short Polling**: Opening a new TCP/TLS connection every 500ms has lower network overhead than persistent streaming",
                "explanation": "Polling every 500ms wastes immense HTTP header and TLS handshake overhead."
            },
            {
                "id": "opt_udp",
                "text": "**Raw UDP Datagrams via WebRTC Data Channels**",
                "explanation": "Unnecessary complexity for a simple price ticker."
            }
        ],
        "correctOptionIds": ["opt_sse"],
        "explanation": "For unidirectional server-to-client streaming, Server-Sent Events (SSE) is the optimal architectural choice: it runs over standard HTTP, multiplexes over HTTP/2, requires no custom socket protocol upgrade, and features native browser reconnection.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Behavioral Interview: The STAR Method Structure",
        "slug": "behavioral-interview-star-method-structure",
        "content": """When answering behavioral and scenario interview questions regarding engineering conflicts, project setbacks, or leadership:

What do the letters in the industry-standard **STAR method** represent?

> [!TIP]
> The recommended structure for articulating concrete past engineering experiences.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Communication"],
        "tags": ["behavioral", "communication", "star-method", "interview-prep"],
        "options": [
            {
                "id": "opt_star",
                "text": "**S**ituation (Context), **T**ask (Responsibility/Goal), **A**ction (Specific steps you personally took), **R**esult (Quantified outcome and learnings)",
                "explanation": "STAR provides a compelling, evidence-based narrative demonstrating ownership, analytical reasoning, and measurable business impact."
            },
            {
                "id": "opt_bad1",
                "text": "**S**oftware, **T**echnology, **A**rchitecture, **R**efactoring",
                "explanation": "These are technical topics, not the behavioral answer framework."
            },
            {
                "id": "opt_bad2",
                "text": "**S**cope, **T**imeline, **A**gile, **R**elease",
                "explanation": "Incorrect definition."
            },
            {
                "id": "opt_bad3",
                "text": "**S**cale, **T**hroughput, **A**vailability, **R**esilience",
                "explanation": "These are non-functional system requirements."
            }
        ],
        "correctOptionIds": ["opt_star"],
        "explanation": "The STAR framework stands for **Situation**, **Task**, **Action**, and **Result**. It ensures interview answers focus on personal actions, technical decisions, and measurable outcomes rather than vague generalities.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Root Cause Analysis: Blameless Postmortems and 5 Whys",
        "slug": "root-cause-analysis-blameless-postmortems",
        "content": """Which principles are foundational to conducting an effective **Blameless Postmortem and Root Cause Analysis (RCA)** after a high-severity production outage?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Communication"],
        "tags": ["postmortem", "rca", "5-whys", "culture", "incident-management"],
        "options": [
            {
                "id": "opt_systemic",
                "text": "Focus on **systemic vulnerabilities, tooling gaps, and missing guardrails** rather than assigning personal blame to the engineer who executed the deploy command",
                "explanation": "Assuming engineers act with good intentions leads to identifying systemic safeguards rather than hiding mistakes."
            },
            {
                "id": "opt_whys",
                "text": "Use the **5 Whys technique** to drill down from the immediate trigger to the underlying organizational or architectural root cause",
                "explanation": "Successive 'Why' questions reveal systemic failures (e.g. why did the test suite fail to catch it? why was canary missing?)."
            },
            {
                "id": "opt_punish",
                "text": "Institute punitive measures and public reprimands to deter engineers from making mistakes in production",
                "explanation": "Punitive cultures encourage engineers to conceal errors and hide incidents, drastically increasing systemic risk."
            },
            {
                "id": "opt_action",
                "text": "Produce concrete, prioritized, trackable **action items** with assigned owners and deadlines to prevent recurrence",
                "explanation": "A postmortem is useless without actionable preventative engineering tasks."
            }
        ],
        "correctOptionIds": ["opt_systemic", "opt_whys", "opt_action"],
        "explanation": "A blameless postmortem recognizes that human error is the symptom of a flawed system, not the root cause. It uses the 5 Whys to identify systemic improvements and produces actionable preventative measures.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Premature Optimization in Software Engineering",
        "slug": "premature-optimization-anti-pattern",
        "content": """As Donald Knuth famously stated: *\"Premature optimization is the root of all evil (or at least most of it) in programming.\"*

Architectural optimization without profiling metrics often introduces unnecessary code complexity, reduced readability, and increased defect rates before any actual bottleneck is identified.

Is this statement **True** or **False**?
""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Communication", "Software Architecture & LLD"],
        "tags": ["clean-code", "optimization", "knuth", "profiling"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "Engineers should write clean, idiomatic code first, measure and profile real production workloads second, and optimize only proven hotspots."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "Optimizing code without benchmarking usually degrades maintainability for negligible performance gains."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. Premature optimization sacrifices clarity, flexibility, and testability to solve speculative performance problems. Real engineering practice prioritizes clean design and uses profilers to pinpoint actual bottlenecks.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Heap Dump Analysis: Shallow vs Retained Size",
        "slug": "heap-dump-analysis-shallow-vs-retained-size",
        "content": """When troubleshooting a severe production JVM OutOfMemoryError (`java.lang.OutOfMemoryError: Java heap space`) using a heap dump profiler (such as Eclipse Memory Analyzer / MAT):

What is the fundamental difference between an object's **Shallow Heap Size** and its **Retained Heap Size**?

| Metric | Definition |
| :--- | :--- |
| **Shallow Size** | ? |
| **Retained Size** | ? |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Debugging", "Core Java"],
        "tags": ["memory-leak", "heap-dump", "profiling", "mat", "troubleshooting"],
        "options": [
            {
                "id": "opt_a",
                "text": "**Shallow Size** is the memory consumed by the object itself (its fields and header, e.g. 24 bytes for a HashMap node); **Retained Size** is the total heap memory that would be freed if this object were garbage collected (including all objects solely reachable from it)",
                "explanation": "Retained heap represents the dominator tree footprint: an object with 32 bytes shallow size can retain 500MB of child data in a cache."
            },
            {
                "id": "opt_b",
                "text": "Shallow size is stored in CPU L1 cache, while Retained size is stored on the physical SSD swap partition",
                "explanation": "Both metrics measure heap RAM allocation in the JVM."
            },
            {
                "id": "opt_c",
                "text": "Shallow size measures thread stack depth, while Retained size measures Metaspace class definitions",
                "explanation": "These metrics apply to heap objects, not stack frames or Metaspace."
            },
            {
                "id": "opt_d",
                "text": "Shallow size is only measured for primitive types, while Retained size is only measured for Strings",
                "explanation": "Both metrics apply to any Java object."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "`Shallow Size` is the amount of memory allocated to hold the object instance itself. `Retained Size` is the total memory that would be reclaimed if that object were collected (the sum of shallow sizes of all objects that are only reachable through that object in the dominator tree).",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Technical Trade-Offs: Modular Monolith vs Microservices",
        "slug": "tradeoffs-modular-monolith-vs-microservices",
        "content": """When evaluating architectural trade-offs between a **Modular Monolith** and a **Microservices Architecture** for a new startup team of 6 engineers:

Which trade-offs accurately reflect reality?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Engineering Scenarios & Communication", "Software Architecture & LLD"],
        "tags": ["architecture", "microservices", "modular-monolith", "trade-offs"],
        "options": [
            {
                "id": "opt_overhead",
                "text": "Microservices introduce significant operational complexity (distributed tracing, network latency, partial failures, CI/CD pipelines, eventual consistency, inter-service security) that can paralyze a small engineering team",
                "explanation": "Microservices exchange code complexity for operational and distributed system complexity."
            },
            {
                "id": "opt_modulith",
                "text": "A well-architected **Modular Monolith** (e.g. using Spring Modulith) enforces strict domain boundaries, fast compilation, simple single-binary deployments, and in-memory function calls with ACID transactions",
                "explanation": "Modular monoliths provide domain isolation without distributed network failure overhead."
            },
            {
                "id": "opt_free",
                "text": "Microservices guarantee that all network calls execute with zero latency and 100% reliability",
                "explanation": "The Fallacies of Distributed Computing: network is neither reliable nor zero-latency."
            },
            {
                "id": "opt_scale",
                "text": "Microservices allow independent deployment and independent horizontal autoscaling of resource-intensive sub-domains",
                "explanation": "Microservices enable independent scaling and team boundary scaling at larger organizations."
            }
        ],
        "correctOptionIds": ["opt_overhead", "opt_modulith", "opt_scale"],
        "explanation": "Microservices offer independent deployments and targeted autoscaling, but introduce heavy operational tax, network latency, and distributed transaction complexity. For small teams, a well-factored Modular Monolith is almost always faster to develop, test, and deploy.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    }
]
