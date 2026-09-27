# Project Abstract: Stacked

## High-Performance Coding Assessment & Online Judge Platform

---

### Executive Summary

**Stacked** is a next-generation, high-performance coding assessment and online judge platform architected to streamline problem curation, automated solution validation, and multi-language evaluation. Engineered with **Spring Boot 4 (Java 25)** on the backend and **React 19 (TypeScript, TanStack Router, TanStack Query, Tailwind CSS v4, Monaco Editor)** on the frontend, Stacked addresses the critical shortcomings of legacy assessment platforms: sluggish problem creation workflows, poor candidate code authoring ergonomics, lack of deterministic multi-dialect database validation, and monolithic, hard-to-scale code runners.

The platform provides a dual-tiered architecture tailored for both assessment administrators and developer candidates. For administrators and problem authors, Stacked delivers a specialized **Coding Problem Studio**—a zero-scroll, fixed-viewport IDE that unifies LaTeX-enabled Markdown problem statement authoring, dynamic sample testcase authoring, and synchronized multi-language starter code, reference solution, and test validator creation. For execution and evaluation, Stacked introduces an isolated, sandboxed execution pipeline supporting generic algorithmic languages (Java, C++, Python, Go, Rust, TypeScript, JavaScript) and database engines (MySQL, PostgreSQL, SQLite) with automated multiset equality, sequential matching, and custom SQL assertion strategies.

---

### Motivation & Problem Statement

Modern technical recruitment and competitive programming platforms struggle with several architectural and operational bottlenecks:

1. **Fragmented Problem Curation**: Authoring an algorithmic problem typically requires switching between disjointed markdown editors, code snippet repositories, and external test runners. Discrepancies between starter code templates, reference solutions, and test validators lead to broken test cases and negative candidate experiences.
2. **Database Problem Evaluation Limitations**: Most online judges treat database problems as plain text comparisons or rely on rigid order-sensitive result set matching. Real-world SQL queries frequently yield identical result multisets through valid alternative execution plans that differ in row order unless explicit sorting is requested.
3. **Ergonomic Deficiency in Problem Authoring**: Traditional problem creation forms suffer from excessive vertical scrolling, scattered form controls, and desynchronized language versions. Authors lack real-time feedback on whether their reference solution passes their own testcases across supported compilers.
4. **Security and Resource Isolation in Code Runners**: Running untrusted user code requires strict kernel-level resource constraints (CPU, memory, disk I/O, process count, and network egress) while minimizing cold-start latencies.
5. **Storage Bloat from Abandoned Drafts**: Image uploads and media assets embedded during problem drafting often remain orphaned in object storage if the author abandons the draft, inflating operational costs.

Stacked solves these challenges through cohesive end-to-end design, combining a full-featured studio with a deterministic validation engine and polyglot persistence.

---

### Key Innovations & Features

- **Zero-Scroll Studio IDE**: A fixed-viewport workbench with resizable Monaco editor panels (`react-resizable-panels`) that eliminates outer page scrollbars and presents reference solutions, starter templates, and testcase inputs side-by-side.
- **Flattened Compiler Registry**: Transparent, compiler-specific runtime targets (e.g., `Java (OpenJDK 21)`, `Python (CPython 3.12)`, `C++ (GCC 13.2)`, `MySQL (8.0)`) eliminating version mismatches between client authoring and backend execution containers.
- **Dual-Stream Solution Validation Protocol**: A deterministic inter-process communication protocol piping testcase input and solution output across standard input separated by a standardized delimiter (`\n---OUTPUT---\n`), validating correctness via exit codes (`Exit 0 = Accepted`).
- **Tri-Mode Database Evaluation Engine**: Built-in support for:
  - _Order-Insensitive Multiset Verification_ (matches row frequency counts regardless of physical row order).
  - _Order-Sensitive Sequence Verification_ (enforces exact row order for queries requiring `ORDER BY`).
  - _Custom Query Assertion_ (executes arbitrary SQL verification scripts for complex schema mutations).
- **Two-Phase Ephemeral File Lifecycle**: Uncommitted markdown image uploads reside in temporary storage backed by automated TTL expiration (`FileCleanupScheduler`). Upon problem publication, referenced assets are converted to permanent storage, eliminating storage waste.
- **Polyglot Persistence Layer**: Combines **MongoDB** for flexible, polymorphic problem documents, testcases, and user profiles with **Redis** for distributed token blacklisting, session management, and sub-millisecond execution caching.

---

### Target Audience & Use Cases

- **Enterprise Engineering Hiring**: High-fidelity technical assessments with realistic multi-file testcase inputs and verifiable reference solutions.
- **University & Academy Education**: Computer science laboratory testing, auto-graded assignments, and database management system (DBMS) query labs.
- **Competitive Programming Platforms**: Rapid hosting of algorithmic contests with low-latency judge verdicts and comprehensive execution metrics.

---

### Visual Architecture Overview

![Stacked System Architecture](assets/system_architecture.jpg)
