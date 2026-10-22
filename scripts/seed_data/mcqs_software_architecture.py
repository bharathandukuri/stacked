"""Software Architecture and Low-Level Design MCQs."""

QUESTIONS = [
    {
        "title": "SOLID Principles: Single Responsibility Principle",
        "slug": "solid-single-responsibility-principle",
        "content": """As originally defined by Robert C. Martin (Uncle Bob), what is the definitive meaning of the **Single Responsibility Principle (SRP)**?

> [!NOTE]
> Often paraphrased in terms of reasons to change.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Software Architecture & LLD"],
        "tags": ["solid", "srp", "design-principles", "clean-code"],
        "options": [
            {
                "id": "opt_a",
                "text": "A module or class should have one, and only one, reason to change (it should be responsible to one, and only one, actor or stakeholder)",
                "explanation": "SRP is about actors and cohesive reasons for change, not restricting classes to a single method."
            },
            {
                "id": "opt_b",
                "text": "A class must never contain more than one public method",
                "explanation": "SRP does not mean single-method classes; a class can have multiple methods that serve the same singular actor."
            },
            {
                "id": "opt_c",
                "text": "Every microservice should have exactly one database table",
                "explanation": "SRP applies at class, module, and architecture levels, but does not mandate 1 table per microservice."
            },
            {
                "id": "opt_d",
                "text": "A function must never call another function outside its own package",
                "explanation": "Incorrect definition."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "The Single Responsibility Principle asserts that a software module should have one, and only one, reason to change. Stated in terms of people, it means a module should be responsible to only one actor (e.g. accounting vs inventory).",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Liskov Substitution Principle and Class Hierarchies",
        "slug": "liskov-substitution-principle-class-hierarchies",
        "content": """The classic object-oriented example of creating a `Square` subclass inheriting from a `Rectangle` base class:

```java
class Rectangle {
    protected int width, height;
    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int getArea() { return width * height; }
}

class Square extends Rectangle {
    @Override public void setWidth(int w) { this.width = w; this.height = w; }
    @Override public void setHeight(int h) { this.width = h; this.height = h; }
}
```

Why does this design violate the **Liskov Substitution Principle (LSP)**?

```java
void testArea(Rectangle r) {
    r.setWidth(5);
    r.setHeight(4);
    assert r.getArea() == 20; // Fails when passed a Square!
}
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Software Architecture & LLD"],
        "tags": ["solid", "lsp", "oop", "polymorphism"],
        "options": [
            {
                "id": "opt_1",
                "text": "Subtypes must be substitutable for their base types without altering the correctness or expectations of client code; `Square` breaks the invariant that setting width does not modify height",
                "explanation": "A client holding a Rectangle reference expects independent dimensions. Overriding setWidth to also mutate height breaks contract post-conditions."
            },
            {
                "id": "opt_2",
                "text": "Java does not support overriding both getters and setters in a subclass",
                "explanation": "Java fully supports overriding methods."
            },
            {
                "id": "opt_3",
                "text": "Square uses more memory on the heap than Rectangle",
                "explanation": "Both classes inherit the same two integer fields."
            },
            {
                "id": "opt_4",
                "text": "Inheritance cannot be used for geometric shapes",
                "explanation": "Inheritance can be used, but models must respect behavioral subtyping, not just natural language taxonomy."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "LSP states that if $S$ is a subtype of $T$, objects of type $T$ may be replaced with objects of type $S$ without altering any desirable properties of the program. Because `Square` changes the behavioral post-conditions of `setWidth`, it cannot cleanly substitute for `Rectangle`.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Strategy Pattern vs State Pattern",
        "slug": "strategy-pattern-vs-state-pattern",
        "content": """Both the **Strategy Pattern** and the **State Pattern** share nearly identical UML class diagrams (a Context delegating to an Interface with multiple concrete implementations).

What is the fundamental **behavioral difference** in their intent?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Software Architecture & LLD"],
        "tags": ["design-patterns", "strategy", "state", "gof"],
        "options": [
            {
                "id": "opt_strategy",
                "text": "In the **Strategy Pattern**, the client usually chooses and configures the specific algorithm/strategy upfront (e.g. `PaymentStrategy`), and the strategy typically does not change during the context's lifecycle",
                "explanation": "Strategy represents alternative interchangeable algorithms."
            },
            {
                "id": "opt_state",
                "text": "In the **State Pattern**, the context dynamically transitions from one state to another internally based on actions, and the concrete states are often aware of each other to trigger transitions",
                "explanation": "State represents changing internal status where behavior changes as state transitions occur."
            },
            {
                "id": "opt_private",
                "text": "Strategy pattern can only be implemented using static methods",
                "explanation": "Strategy uses polymorphism and objects."
            },
            {
                "id": "opt_state_intent",
                "text": "The State pattern eliminates complex nested `if-else` or `switch` statements by encapsulating state-dependent behavior in dedicated classes",
                "explanation": "State pattern replaces switch(status) branches with polymorphic state dispatch."
            }
        ],
        "correctOptionIds": ["opt_strategy", "opt_state", "opt_state_intent"],
        "explanation": "Strategy encapsulates interchangeable algorithms configured by the client. State encapsulates state-specific behavior where the context automatically transitions between states in response to events.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Microservices Distributed Transactions: Saga Pattern",
        "slug": "saga-pattern-microservices-transactions",
        "content": """In a microservices architecture spanning multiple autonomous service databases (e.g. Order Service, Payment Service, Inventory Service):

Why is the **Saga Pattern** preferred over traditional **Two-Phase Commit (2PC / XA)** across distributed services?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Software Architecture & LLD", "Backend Engineering & Spring"],
        "tags": ["microservices", "saga", "distributed-transactions", "2pc"],
        "options": [
            {
                "id": "opt_lock",
                "text": "2PC is a blocking protocol: if the transaction coordinator or any participant crashes during the prepare phase, resources remain locked, degrading throughput and availability",
                "explanation": "2PC coordinator failure can leave locks hanging indefinitely."
            },
            {
                "id": "opt_comp",
                "text": "Sagas break transactions into a sequence of local ACID transactions and rely on **compensating transactions** (semantic undo) if a downstream step fails",
                "explanation": "Sagas provide eventual consistency without holding distributed locks across services."
            },
            {
                "id": "opt_cloud",
                "text": "Many modern databases, message brokers, and cloud NoSQL databases do not support XA/2PC protocols across independent network boundaries",
                "explanation": "XA support across heterogeneous cloud technologies is rare or discouraged."
            },
            {
                "id": "opt_speed",
                "text": "2PC makes network latency 0ms by caching responses",
                "explanation": "2PC increases latency due to multiple network round-trips."
            }
        ],
        "correctOptionIds": ["opt_lock", "opt_comp", "opt_cloud"],
        "explanation": "2PC holds locks across multiple independent services, crippling availability and scalability. The Saga pattern executes a series of local transactions coordinated via events (Choreography) or an orchestrator (Orchestration), using compensating transactions to undo steps if failure occurs.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Consistent Hashing with Virtual Nodes",
        "slug": "consistent-hashing-virtual-nodes",
        "content": """In distributed caching systems (like DynamoDB or Memcached clusters):

What critical problem does **Consistent Hashing with Virtual Nodes (vnodes)** solve compared to naive modulo hashing ($hash(key) \\pmod N$)?

> [!TIP]
> Consider what happens when a node is added or removed from a cluster of $N$ servers.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Software Architecture & LLD", "NoSQL & Distributed Caching"],
        "tags": ["consistent-hashing", "distributed-systems", "sharding", "virtual-nodes"],
        "options": [
            {
                "id": "opt_a",
                "text": "When a node is added or removed, consistent hashing remaps only $K / N$ keys on average (where $K$ is total keys and $N$ is server count); virtual nodes distribute the keys evenly to prevent hot-spot load skew",
                "explanation": "Naive modulo hashing remaps almost 100% of keys when N changes. Consistent hashing maps keys to a hash ring, moving only a fraction of keys, and virtual nodes ensure uniform distribution across servers."
            },
            {
                "id": "opt_b",
                "text": "Consistent hashing completely eliminates the need for replication",
                "explanation": "Replication is still necessary for fault tolerance."
            },
            {
                "id": "opt_c",
                "text": "Virtual nodes encrypt the stored keys to comply with GDPR",
                "explanation": "Virtual nodes are a load balancing mechanism, not cryptographic encryption."
            },
            {
                "id": "opt_d",
                "text": "Naive modulo hashing has $O(1)$ lookup while consistent hashing takes $O(N^2)$ time",
                "explanation": "Consistent hashing with binary search takes O(log V) time where V is virtual node count."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "Naive $hash(key) \\pmod N$ invalidates almost all keys when $N$ changes. Consistent hashing places nodes on a $360^\\circ$ hash ring so adding/removing a node only shifts keys between immediate neighbors ($K/N$ remapped). Virtual nodes map each physical machine to multiple ring points to ensure uniform load balance.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "High Cohesion and Low Coupling Invariant",
        "slug": "high-cohesion-low-coupling-invariant",
        "content": """In software engineering architecture:

A system characterized by **High Cohesion** (elements within a module strongly related in purpose) and **Low Coupling** (minimal inter-module dependencies) is easier to maintain, test, and evolve than a system with Low Cohesion and High Coupling.

Is this statement **True** or **False**?
""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Software Architecture & LLD"],
        "tags": ["architecture", "cohesion", "coupling", "clean-architecture"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "High cohesion ensures modules do one thing well; low coupling minimizes ripple effects when one module changes."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "High coupling and low cohesion are the hallmarks of brittle spaghetti architecture."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. High cohesion ensures that related responsibilities reside together. Low coupling ensures modules interact through well-defined interfaces without depending on internal implementation details, isolating the impact of changes.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Open/Closed Principle and Extension Mechanisms",
        "slug": "open-closed-principle-extension-mechanisms",
        "content": """The **Open/Closed Principle (OCP)** states: *\"Software entities (classes, modules, functions) should be open for extension, but closed for modification.\"*

Which design pattern best demonstrates OCP by allowing new payment processors (e.g. PayPal, Stripe, Apple Pay) to be added without modifying the existing `CheckoutService` code?

```java
// How can CheckoutService be closed for modification while open for new processors?
public class CheckoutService {
    public void process(PaymentStrategy strategy, double amount) {
        strategy.pay(amount); // Polymorphic dispatch
    }
}
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Software Architecture & LLD"],
        "tags": ["solid", "ocp", "design-patterns", "strategy"],
        "options": [
            {
                "id": "opt_strategy",
                "text": "**Strategy Pattern / Polymorphic Interfaces**: Defining a `PaymentStrategy` interface allows new payment providers to be added by implementing the interface without altering existing `CheckoutService` code",
                "explanation": "Adding a new implementation does not touch the existing service, satisfying OCP."
            },
            {
                "id": "opt_switch",
                "text": "Using a central `switch(paymentType)` statement with 20 `case` branches inside `CheckoutService`",
                "explanation": "A central switch requires modifying CheckoutService on every new payment provider, directly violating OCP."
            },
            {
                "id": "opt_singleton",
                "text": "Converting `CheckoutService` into a global Singleton with static methods",
                "explanation": "Singletons have nothing to do with extension without modification."
            },
            {
                "id": "opt_cast",
                "text": "Using downcasting and `instanceof` checks in every method",
                "explanation": "Downcasting violates polymorphism and OCP."
            }
        ],
        "correctOptionIds": ["opt_strategy"],
        "explanation": "The Open/Closed Principle is achieved via abstraction and polymorphism. By depending on a `PaymentStrategy` interface rather than concrete classes, new processors can be created without changing tested service code.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Dependency Inversion Principle: Abstractions Over Details",
        "slug": "dependency-inversion-principle-abstractions",
        "content": """In the **Dependency Inversion Principle (DIP)** (the 'D' in SOLID):

1. High-level policy modules should not depend on low-level implementation modules. Both should depend on **abstractions**.
2. Abstractions should not depend on details. Details (concrete implementations) should depend on **abstractions**.

Is this statement **True** or **False**?
""",
        "type": "BOOLEAN",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Software Architecture & LLD"],
        "tags": ["solid", "dip", "dependency-inversion", "architecture"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "DIP reverses the traditional top-down procedural dependency structure by placing interfaces between high-level policy and low-level details."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "This is the exact formal definition of the Dependency Inversion Principle."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. The Dependency Inversion Principle ensures that business logic (high-level policy) is decoupled from specific database engines, message queues, or UI frameworks (low-level details). Both depend on interfaces.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    }
]
