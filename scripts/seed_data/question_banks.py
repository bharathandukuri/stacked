"""
Question Bank specifications for Stacked.
Curates 5 production-quality question banks across various roles and experience levels.
Items reference question slugs and are mapped to real MongoDB IDs during seeding.
"""

BANKS = [
    {
        "title": "Junior Full-Stack Engineer Screening",
        "description": "Comprehensive screening assessment for entry-level and junior full-stack developers. Evaluates core programming fundamentals, basic data structures, DOM/React front-end concepts, and foundational SQL querying.",
        "category": "Full-Stack",
        "tags": ["Junior", "Full-Stack", "JavaScript", "React", "Python", "SQL", "Screening"],
        "status": "PUBLISHED",
        "items": [
            {"type": "MCQ", "slug": "short-circuit-logical-evaluation", "customPoints": 5.0},
            {"type": "MCQ", "slug": "pass-by-value-vs-object-references", "customPoints": 5.0},
            {"type": "MCQ", "slug": "javascript-closures-in-asynchronous-loops", "customPoints": 5.0},
            {"type": "MCQ", "slug": "javascript-event-loop-execution-order", "customPoints": 5.0},
            {"type": "MCQ", "slug": "react-keys-and-fiber-reconciliation", "customPoints": 5.0},
            {"type": "MCQ", "slug": "http-status-codes-401-vs-403", "customPoints": 5.0},
            {"type": "MCQ", "slug": "left-join-converted-to-inner-join-where", "customPoints": 5.0},
            {"type": "MCQ", "slug": "git-branching-merge-vs-rebase", "customPoints": 5.0},
            {"type": "CODING", "slug": "fizz-buzz-custom", "customPoints": 15.0},
            {"type": "CODING", "slug": "two-sum", "customPoints": 20.0},
            {"type": "CODING", "slug": "valid-palindrome", "customPoints": 15.0},
            {"type": "CODING", "slug": "sql-second-highest-salary", "customPoints": 15.0},
        ]
    },
    {
        "title": "Core Java & Spring Boot Backend Engineer Assessment",
        "description": "Rigorous technical evaluation for mid-to-senior Java and Spring backend engineers. Covers JVM memory model, Java concurrency & atomics, Spring Bean lifecycle & transactional propagation, collections, and backend system integration.",
        "category": "Backend",
        "tags": ["Java", "Spring Boot", "Backend", "Microservices", "Concurrency", "OOP"],
        "status": "PUBLISHED",
        "items": [
            {"type": "MCQ", "slug": "string-literal-pool-reference-equality", "customPoints": 5.0},
            {"type": "MCQ", "slug": "equals-and-hashcode-contract-invariants", "customPoints": 5.0},
            {"type": "MCQ", "slug": "try-with-resources-close-order", "customPoints": 5.0},
            {"type": "MCQ", "slug": "volatile-modifier-and-jmm", "customPoints": 5.0},
            {"type": "MCQ", "slug": "concurrenthashmap-thread-safety-mechanism", "customPoints": 5.0},
            {"type": "MCQ", "slug": "virtual-threads-vs-platform-threads-java21", "customPoints": 5.0},
            {"type": "MCQ", "slug": "java-streams-lazy-intermediate-operations", "customPoints": 5.0},
            {"type": "MCQ", "slug": "spring-di-prototype-in-singleton-pitfall", "customPoints": 5.0},
            {"type": "MCQ", "slug": "spring-transactional-self-invocation-rollback", "customPoints": 5.0},
            {"type": "MCQ", "slug": "spring-boot-autoconfiguration-conditional-annotations", "customPoints": 5.0},
            {"type": "CODING", "slug": "longest-substring-without-repeating-characters", "customPoints": 20.0},
            {"type": "CODING", "slug": "merge-intervals", "customPoints": 20.0},
            {"type": "CODING", "slug": "lru-cache-simulation", "customPoints": 25.0},
            {"type": "CODING", "slug": "sql-customer-high-value-orders", "customPoints": 20.0},
        ]
    },
    {
        "title": "Data Structures & Algorithms Mastery",
        "description": "In-depth problem-solving assessment spanning foundational to advanced DSA concepts: two pointers, sliding windows, monotonic stacks, trees, graphs, dynamic programming, and custom judge verifiers.",
        "category": "Computer Science",
        "tags": ["DSA", "Algorithms", "Data Structures", "Competitive Programming", "Optimization"],
        "status": "PUBLISHED",
        "items": [
            {"type": "MCQ", "slug": "binary-search-tree-degeneration-asymptotics", "customPoints": 5.0},
            {"type": "MCQ", "slug": "dynamic-array-resizing-amortized-complexity", "customPoints": 5.0},
            {"type": "MCQ", "slug": "monotonic-stack-next-greater-element-complexity", "customPoints": 5.0},
            {"type": "MCQ", "slug": "hash-table-collision-resolution-strategies", "customPoints": 5.0},
            {"type": "MCQ", "slug": "knapsack-problem-dp-vs-greedy", "customPoints": 5.0},
            {"type": "MCQ", "slug": "binary-search-on-rotated-sorted-array", "customPoints": 5.0},
            {"type": "MCQ", "slug": "topological-sort-and-cycle-detection", "customPoints": 5.0},
            {"type": "MCQ", "slug": "disjoint-set-union-find-complexity", "customPoints": 5.0},
            {"type": "CODING", "slug": "classic-binary-search", "customPoints": 10.0},
            {"type": "CODING", "slug": "valid-parentheses", "customPoints": 10.0},
            {"type": "CODING", "slug": "maximum-subarray-kadane", "customPoints": 15.0},
            {"type": "CODING", "slug": "min-stack-design", "customPoints": 20.0},
            {"type": "CODING", "slug": "coin-change-minimum-coins", "customPoints": 20.0},
            {"type": "CODING", "slug": "trapping-rain-water", "customPoints": 30.0},
            {"type": "CODING", "slug": "distinct-prime-factorization-verifier", "customPoints": 25.0},
        ]
    },
    {
        "title": "Database Systems & Data Engineering Assessment",
        "description": "Specialized assessment for data engineers, database administrators, and data-intensive application developers. Covers relational query optimization, window functions, ACID isolation anomalies, NoSQL document modeling, and Redis caching architectures.",
        "category": "Data Engineering",
        "tags": ["Database", "SQL", "PostgreSQL", "MySQL", "SQLite", "MongoDB", "Redis"],
        "status": "PUBLISHED",
        "items": [
            {"type": "MCQ", "slug": "acid-properties-write-ahead-logging", "customPoints": 5.0},
            {"type": "MCQ", "slug": "sql-transaction-isolation-levels-anomalies", "customPoints": 5.0},
            {"type": "MCQ", "slug": "sql-three-valued-logic-null-comparisons", "customPoints": 5.0},
            {"type": "MCQ", "slug": "composite-btree-index-leftmost-prefix-rule", "customPoints": 5.0},
            {"type": "MCQ", "slug": "window-functions-rank-denserank-rownumber", "customPoints": 5.0},
            {"type": "MCQ", "slug": "database-deadlock-prevention-lock-ordering", "customPoints": 5.0},
            {"type": "MCQ", "slug": "redis-data-structure-selection", "customPoints": 5.0},
            {"type": "MCQ", "slug": "cache-stampede-mitigation", "customPoints": 5.0},
            {"type": "MCQ", "slug": "mongodb-unbounded-arrays-anti-pattern", "customPoints": 5.0},
            {"type": "MCQ", "slug": "mongodb-write-concern-majority-journaling", "customPoints": 5.0},
            {"type": "CODING", "slug": "sql-second-highest-salary", "customPoints": 15.0},
            {"type": "CODING", "slug": "sql-department-highest-salary", "customPoints": 20.0},
            {"type": "CODING", "slug": "sql-customer-high-value-orders", "customPoints": 20.0},
            {"type": "CODING", "slug": "sql-department-top-three-salaries", "customPoints": 25.0},
            {"type": "CODING", "slug": "mongo-ecommerce-category-revenue", "customPoints": 25.0},
        ]
    },
    {
        "title": "Senior Software Architect & Systems Engineering",
        "description": "Comprehensive evaluation for senior and lead engineers. Assesses low-level and high-level architectural patterns, distributed system trade-offs, OS internals, containerization, and engineering leadership scenarios.",
        "category": "System Design",
        "tags": ["System Design", "Architecture", "Distributed Systems", "Linux", "Docker", "Senior"],
        "status": "PUBLISHED",
        "items": [
            {"type": "MCQ", "slug": "solid-single-responsibility-principle", "customPoints": 5.0},
            {"type": "MCQ", "slug": "liskov-substitution-principle-class-hierarchies", "customPoints": 5.0},
            {"type": "MCQ", "slug": "saga-pattern-microservices-transactions", "customPoints": 5.0},
            {"type": "MCQ", "slug": "consistent-hashing-virtual-nodes", "customPoints": 5.0},
            {"type": "MCQ", "slug": "os-process-vs-thread-address-space", "customPoints": 5.0},
            {"type": "MCQ", "slug": "tcp-4way-handshake-and-time-wait", "customPoints": 5.0},
            {"type": "MCQ", "slug": "dockerfile-layer-caching-optimization", "customPoints": 5.0},
            {"type": "MCQ", "slug": "incident-database-connection-pool-exhaustion", "customPoints": 5.0},
            {"type": "MCQ", "slug": "architectural-tradeoffs-websockets-sse-polling", "customPoints": 5.0},
            {"type": "MCQ", "slug": "behavioral-interview-star-method-structure", "customPoints": 5.0},
            {"type": "CODING", "slug": "rate-limiter-token-bucket", "customPoints": 25.0},
            {"type": "CODING", "slug": "lru-cache-simulation", "customPoints": 25.0},
            {"type": "CODING", "slug": "trapping-rain-water", "customPoints": 30.0},
            {"type": "CODING", "slug": "median-of-two-sorted-arrays", "customPoints": 35.0},
        ]
    }
]

