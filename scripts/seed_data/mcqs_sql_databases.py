"""SQL and Relational Databases MCQs."""

QUESTIONS = [
    {
        "title": "LEFT JOIN Converted to INNER JOIN via WHERE Clause",
        "slug": "left-join-converted-to-inner-join-where",
        "content": """Consider the following SQL query against an e-commerce database:

```sql
SELECT c.name, o.amount
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id
WHERE o.status = 'COMPLETED';
```

What happens to customers who have **never placed an order** (`orders.customer_id IS NULL`)?

| `customers` table | `orders` table | Expected Join Intent |
| :--- | :--- | :--- |
| Contains customers without orders | Contains completed and pending orders | Intended outer join |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["SQL & Relational Databases"],
        "tags": ["sql", "joins", "left-join", "filtering"],
        "options": [
            {
                "id": "opt_a",
                "text": "They are completely excluded from the result set because `NULL = 'COMPLETED'` evaluates to `UNKNOWN`, which the `WHERE` clause filters out",
                "explanation": "Placing a filter on the right-side table in the WHERE clause (instead of the ON clause) effectively converts the LEFT JOIN into an INNER JOIN."
            },
            {
                "id": "opt_b",
                "text": "They appear in the results with `amount = NULL` and `status = 'COMPLETED'`",
                "explanation": "NULL values cannot satisfy equality checks."
            },
            {
                "id": "opt_c",
                "text": "A SQL syntax error is raised because outer tables cannot be filtered in WHERE",
                "explanation": "The query is valid SQL syntax."
            },
            {
                "id": "opt_d",
                "text": "The database raises a runtime constraint violation",
                "explanation": "No constraint violation occurs; rows are simply filtered."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "In SQL, placing a predicate on the non-preserved table of a `LEFT JOIN` in the `WHERE` clause forces any unmatched rows (which have `o.status IS NULL`) to be eliminated because `NULL = 'COMPLETED'` evaluates to `UNKNOWN` (treated as false in WHERE). To preserve customers without orders, move the filter to the `ON` clause: `ON c.id = o.customer_id AND o.status = 'COMPLETED'`.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "ACID Properties: Write-Ahead Logging (WAL)",
        "slug": "acid-properties-write-ahead-logging",
        "content": """How do modern relational database engines (PostgreSQL, MySQL InnoDB, SQLite) guarantee the **Atomicity** and **Durability** of transactions during unexpected system crashes?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["SQL & Relational Databases"],
        "tags": ["acid", "transactions", "wal", "durability", "internals"],
        "options": [
            {
                "id": "opt_1",
                "text": "**Write-Ahead Logging (WAL)**: Log records describing data changes must be flushed to persistent non-volatile storage *before* corresponding dirty database pages are written to data files",
                "explanation": "This guarantees that if a crash occurs, committed changes can be redone and uncommitted changes can be undone."
            },
            {
                "id": "opt_2",
                "text": "**Redo Logs**: Allow the database crash-recovery manager to replay committed transactions that had not yet reached dirty disk pages",
                "explanation": "Redo logs ensure Durability."
            },
            {
                "id": "opt_3",
                "text": "**Undo Logs**: Allow the engine to roll back uncommitted or aborted transaction modifications, ensuring Atomicity",
                "explanation": "Undo logs ensure Atomicity."
            },
            {
                "id": "opt_4",
                "text": "Databases never write dirty pages to disk until the physical server is cleanly shut down",
                "explanation": "Dirty pages are continuously flushed via background checkpoint threads."
            }
        ],
        "correctOptionIds": ["opt_1", "opt_2", "opt_3"],
        "explanation": "The fundamental rule of Write-Ahead Logging (WAL) is that log records must reach disk before dirty data pages are flushed. Redo logs ensure Durability (replaying committed work), while Undo logs ensure Atomicity (reverting incomplete work).",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "SQL Transaction Isolation Levels and Anomalies",
        "slug": "sql-transaction-isolation-levels-anomalies",
        "content": """In the ANSI/ISO SQL standard for transaction isolation levels:

Which anomaly is permitted under **Read Committed**, but strictly prevented under **Repeatable Read**?

> [!NOTE]
> Consider standard SQL definitions of Dirty Read, Non-Repeatable Read, and Phantom Read.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["SQL & Relational Databases"],
        "tags": ["isolation-levels", "transactions", "concurrency", "acid"],
        "options": [
            {
                "id": "opt_a",
                "text": "**Non-Repeatable Read (Fuzzy Read)**: Transaction T1 reads a row, T2 updates and commits that row, and T1 re-reads the row obtaining the updated value",
                "explanation": "Read Committed allows reading newly committed row updates, producing different values within the same transaction. Repeatable Read guarantees snapshot stability for existing rows."
            },
            {
                "id": "opt_b",
                "text": "**Dirty Read**: Transaction T1 reads uncommitted changes from concurrent transaction T2",
                "explanation": "Dirty Read is already prevented at Read Committed."
            },
            {
                "id": "opt_c",
                "text": "**Lost Update**: When two transactions overwrite each other simultaneously",
                "explanation": "Lost updates are prevented by write locks at Read Committed."
            },
            {
                "id": "opt_d",
                "text": "**Phantom Read**: T1 reads a range of rows, T2 inserts a new row satisfying the predicate and commits, and T1 re-reads seeing the new row",
                "explanation": "ANSI SQL Repeatable Read allows Phantom Reads (prevented under Serializable), though engines like InnoDB and Postgres prevent phantoms in Repeatable Read via MVCC/Next-Key locks."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "Under Read Committed, re-reading an existing row can yield different data if another transaction modified and committed it (Non-Repeatable Read). Repeatable Read prevents this anomaly.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "SQL Three-Valued Logic and NULL Comparisons",
        "slug": "sql-three-valued-logic-null-comparisons",
        "content": """In ANSI SQL three-valued logic (`TRUE`, `FALSE`, `UNKNOWN`):

What is the result of evaluating the expression `NULL = NULL`?

```sql
SELECT CASE 
    WHEN NULL = NULL THEN 'EQUAL' 
    ELSE 'NOT EQUAL / UNKNOWN' 
END;
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["SQL & Relational Databases"],
        "tags": ["sql", "null", "three-valued-logic"],
        "options": [
            {
                "id": "opt_1",
                "text": "`UNKNOWN` (falsy in conditional branch, executing the `ELSE` clause)",
                "explanation": "NULL represents an unknown value. Comparing unknown to unknown yields UNKNOWN, never TRUE."
            },
            {
                "id": "opt_2",
                "text": "`TRUE` (both operands have identical null type)",
                "explanation": "Equality operators (=, !=) with NULL never evaluate to TRUE."
            },
            {
                "id": "opt_3",
                "text": "A runtime `NullPointerException` is thrown by the SQL engine",
                "explanation": "SQL handles NULL natively through three-valued logic."
            },
            {
                "id": "opt_4",
                "text": "`FALSE` strictly",
                "explanation": "The logical evaluation result is UNKNOWN, not boolean FALSE (though conditional clauses treat UNKNOWN as falsy)."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "In SQL, `NULL` denotes missing or unknown data. Any arithmetic or comparison operator with `NULL` (like `NULL = NULL` or `val = NULL`) evaluates to `UNKNOWN`. To test for nullness, `IS NULL` or `IS NOT NULL` must be used.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Composite B-Tree Index Leftmost Prefix Rule",
        "slug": "composite-btree-index-leftmost-prefix-rule",
        "content": """Suppose a relational database table `orders` has a composite B-Tree index on columns `(user_id, status, created_at)`.

Which of the following queries **can fully utilize** this index for filtering?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["SQL & Relational Databases"],
        "tags": ["indexing", "b-tree", "composite-index", "query-optimization"],
        "options": [
            {
                "id": "opt_a",
                "text": "`SELECT * FROM orders WHERE user_id = 42;`",
                "explanation": "Uses the leftmost prefix (user_id)."
            },
            {
                "id": "opt_b",
                "text": "`SELECT * FROM orders WHERE user_id = 42 AND status = 'COMPLETED';`",
                "explanation": "Uses the leftmost two prefix columns (user_id, status)."
            },
            {
                "id": "opt_c",
                "text": "`SELECT * FROM orders WHERE status = 'COMPLETED' AND created_at > '2026-01-01';`",
                "explanation": "Cannot use the index because the leading prefix column (user_id) is missing."
            },
            {
                "id": "opt_d",
                "text": "`SELECT * FROM orders WHERE user_id = 42 AND status = 'COMPLETED' AND created_at > '2026-01-01';`",
                "explanation": "Uses all three indexed columns in prefix order."
            }
        ],
        "correctOptionIds": ["opt_a", "opt_b", "opt_d"],
        "explanation": "Composite B-Tree indexes are ordered lexicographically by the leftmost column first. A query can only leverage the index if it filters on a continuous leading prefix of the indexed columns (e.g., `user_id`, or `user_id + status`). Query C omits `user_id`, preventing B-tree prefix traversal.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Window Functions: RANK vs DENSE_RANK vs ROW_NUMBER",
        "slug": "window-functions-rank-denserank-rownumber",
        "content": """Given a salary dataset with values: `[1000, 2000, 2000, 3000]`.

When sorting descending `ORDER BY salary DESC`:

What ranks are assigned to these four rows by `ROW_NUMBER()`, `RANK()`, and `DENSE_RANK()`?

```sql
SELECT salary,
       ROW_NUMBER() OVER (ORDER BY salary DESC) as rn,
       RANK()       OVER (ORDER BY salary DESC) as rk,
       DENSE_RANK() OVER (ORDER BY salary DESC) as dr
FROM employees;
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["SQL & Relational Databases"],
        "tags": ["sql", "window-functions", "rank", "dense-rank"],
        "options": [
            {
                "id": "opt_1",
                "text": "`ROW_NUMBER`: 1, 2, 3, 4 | `RANK`: 1, 2, 2, 4 (gap on tie) | `DENSE_RANK`: 1, 2, 2, 3 (no gaps)",
                "explanation": "ROW_NUMBER assigns unique sequential integers. RANK assigns identical numbers to ties and skips subsequent numbers. DENSE_RANK assigns identical numbers to ties without skipping."
            },
            {
                "id": "opt_2",
                "text": "`ROW_NUMBER`: 1, 2, 2, 3 | `RANK`: 1, 2, 2, 4 | `DENSE_RANK`: 1, 2, 3, 4",
                "explanation": "ROW_NUMBER never assigns duplicates."
            },
            {
                "id": "opt_3",
                "text": "`ROW_NUMBER`: 1, 2, 3, 4 | `RANK`: 1, 2, 2, 3 | `DENSE_RANK`: 1, 2, 2, 4",
                "explanation": "RANK produces gaps; DENSE_RANK does not."
            },
            {
                "id": "opt_4",
                "text": "All three functions produce identical results: 1, 2, 3, 4",
                "explanation": "Ties cause different behavior across the three functions."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "`ROW_NUMBER()` always produces contiguous unique integers ($1, 2, 3, 4$). `RANK()` produces ties and skips ranks ($1, 2, 2, 4$). `DENSE_RANK()` produces ties without skipping ranks ($1, 2, 2, 3$).",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "COUNT(*) vs COUNT(column_name) Null Semantics",
        "slug": "count-star-vs-count-column-null-semantics",
        "content": """In SQL aggregation:

`COUNT(*)` returns the total count of rows retrieved, including rows where every column contains `NULL`.

In contrast, `COUNT(column_name)` counts only rows where `column_name` is strictly non-null (`IS NOT NULL`).

Is this statement **True** or **False**?
""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["SQL & Relational Databases"],
        "tags": ["sql", "aggregation", "count", "null"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "COUNT(*) counts table rows unconditionally. COUNT(col) ignores rows where col is NULL."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "COUNT(col) does not count NULL entries."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. `COUNT(*)` counts table records regardless of column contents. `COUNT(column_name)` evaluates the specified expression for each row and increments only when the result is non-null.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Recursive CTEs for Hierarchical Traversal",
        "slug": "recursive-ctes-for-hierarchical-traversal",
        "content": """Consider the following SQL Common Table Expression (CTE) executed on an organizational employee-manager hierarchy:

```sql
WITH RECURSIVE OrgTree AS (
    -- Anchor Member:
    SELECT id, name, manager_id, 1 AS depth
    FROM employees
    WHERE manager_id IS NULL

    UNION ALL

    -- Recursive Member:
    SELECT e.id, e.name, e.manager_id, o.depth + 1
    FROM employees e
    JOIN OrgTree o ON e.manager_id = o.id
)
SELECT * FROM OrgTree;
```

How does the SQL engine execute this query?

Select the most accurate description:""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["SQL & Relational Databases"],
        "tags": ["sql", "cte", "recursive-cte", "hierarchical-data"],
        "options": [
            {
                "id": "opt_a",
                "text": "The anchor member runs once to seed the working table (root CEO); the recursive member iteratively joins the working table with `employees` to discover the next level of reports until no new rows are produced, then performs a `UNION ALL`",
                "explanation": "Recursive CTEs evaluate the anchor query first, then repeatedly execute the recursive query against the previous intermediate result set until reaching a fixed point (empty delta)."
            },
            {
                "id": "opt_b",
                "text": "The engine compiles the CTE into an infinite Java while-loop that causes a StackOverflowError",
                "explanation": "SQL engines execute recursive CTEs with internal iteration limits or until fixed-point termination."
            },
            {
                "id": "opt_c",
                "text": "It executes a full table cartesian cross join on every iteration",
                "explanation": "The join condition e.manager_id = o.id restricts matches to immediate direct reports."
            },
            {
                "id": "opt_d",
                "text": "Recursive CTEs only work in SQLite; PostgreSQL and MySQL reject the RECURSIVE keyword",
                "explanation": "Recursive CTEs are standard ANSI SQL:1999 and supported in PostgreSQL, MySQL 8+, SQLite, and SQL Server."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "A recursive CTE consists of an Anchor member (which initializes the recursion) and a Recursive member (which queries the intermediate result table). It iterates until the recursive member returns an empty set, modeling graph and tree structures in standard SQL.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Database Deadlock Prevention via Lock Ordering",
        "slug": "database-deadlock-prevention-lock-ordering",
        "content": """In high-concurrency database systems:

A **deadlock** occurs when Transaction 1 holds a lock on Row A and waits for Row B, while Transaction 2 holds a lock on Row B and waits for Row A.

Which programming practice prevents such deadlocks across concurrent application transactions?

```text
Tx 1: LOCK(A) -> LOCK(B)
Tx 2: LOCK(B) -> LOCK(A)  <-- DEADLOCK CYCLIC DEPENDENCY!
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["SQL & Relational Databases", "Core Java"],
        "tags": ["deadlock", "locking", "transactions", "concurrency"],
        "options": [
            {
                "id": "opt_order",
                "text": "**Global Consistent Lock Ordering**: Ensure all application transactions acquire row or table locks in the identical deterministic order (e.g., sorted by primary key ID in ascending order)",
                "explanation": "If all transactions lock resource min(id) before max(id), cyclic wait dependencies are mathematically eliminated (breaking Coffman's circular wait condition)."
            },
            {
                "id": "opt_repeat",
                "text": "Increase transaction isolation level to Serializable",
                "explanation": "Serializable increases lock contention and causes more frequent serialization failures and deadlocks."
            },
            {
                "id": "opt_sleep",
                "text": "Insert a 500ms `Thread.sleep()` before every SQL statement",
                "explanation": "Sleeping merely widens the critical section window, increasing deadlock likelihood."
            },
            {
                "id": "opt_disable",
                "text": "Disable the database engine's wait-for graph deadlock detector",
                "explanation": "Disabling deadlock detection causes transactions to hang indefinitely until timeouts occur."
            }
        ],
        "correctOptionIds": ["opt_order"],
        "explanation": "Deadlocks require circular wait. Enforcing a global deterministic lock acquisition order (such as ordering updates by ascending primary key ID) breaks the circular wait condition and guarantees deadlock prevention.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    }
]
