"""Core Java MCQs."""

QUESTIONS = [
    {
        "title": "String Literal Pool and Reference Equality",
        "slug": "string-literal-pool-reference-equality",
        "content": """Analyze the following Java code snippet regarding string pooling and object allocation:

```java
String s1 = "stacked";
String s2 = "stacked";
String s3 = new String("stacked");
String s4 = s3.intern();

boolean r1 = (s1 == s2);
boolean r2 = (s1 == s3);
boolean r3 = (s1 == s4);
```

What are the values of `r1`, `r2`, and `r3` respectively?

| Comparison | Operands | Checks |
| :--- | :--- | :--- |
| `s1 == s2` | String literals | String Intern Pool identity |
| `s1 == s3` | Literal vs `new String` | Pool vs Separate Heap object |
| `s1 == s4` | Literal vs `.intern()` | Pool vs Canonical Pool reference |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Core Java", "Programming Fundamentals"],
        "tags": ["strings", "interning", "jvm-memory", "identity"],
        "options": [
            {
                "id": "opt_a",
                "text": "`true`, `false`, `true`",
                "explanation": "s1 and s2 reference the identical interned literal in String Constant Pool. s3 is explicitly allocated on heap via `new`. s4 retrieves the interned canonical reference, matching s1."
            },
            {
                "id": "opt_b",
                "text": "`true`, `true`, `true`",
                "explanation": "s1 == s3 is false because `new String()` always forces allocation of a distinct heap object."
            },
            {
                "id": "opt_c",
                "text": "`false`, `false`, `true`",
                "explanation": "s1 == s2 is true because identical string literals within the same class loader resolve to the same pooled instance."
            },
            {
                "id": "opt_d",
                "text": "`true`, `false`, `false`",
                "explanation": "s3.intern() returns the instance from the string pool, making s1 == s4 true."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "In Java, string literals (`\"stacked\"`) are automatically interned in the String Constant Pool, making `s1 == s2` true. Calling `new String(\"stacked\")` forces a new object on the heap, so `s1 == s3` is false. Calling `.intern()` on `s3` returns the canonical pool reference, so `s1 == s4` is true.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "equals() and hashCode() Contract Invariants",
        "slug": "equals-and-hashcode-contract-invariants",
        "content": """According to the Java SE specification for `java.lang.Object`, which statements are **strict requirements** of the `equals()` and `hashCode()` contract?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Core Java"],
        "tags": ["collections", "hashmap", "contract", "equals-hashcode"],
        "options": [
            {
                "id": "opt_1",
                "text": "If two objects are equal according to `equals(Object)`, calling `hashCode()` on each must produce the identical integer",
                "explanation": "Mandatory invariant. Violating this breaks hash-based collections like HashMap and HashSet."
            },
            {
                "id": "opt_2",
                "text": "If two objects have the same `hashCode()`, they must always evaluate to `true` when compared with `equals(Object)`",
                "explanation": "Incorrect. Hash collisions are permitted; distinct objects can yield the same hash code."
            },
            {
                "id": "opt_3",
                "text": "Whenever invoked on the same object multiple times during an execution, `hashCode()` must consistently return the same integer, provided no information used in `equals()` comparisons is modified",
                "explanation": "Mandatory consistency requirement."
            },
            {
                "id": "opt_4",
                "text": "The `equals` method must be reflexive (`x.equals(x) == true`), symmetric (`x.equals(y) == y.equals(x)`), and transitive (`x.equals(y) && y.equals(z) => x.equals(z)`)",
                "explanation": "Mandatory equivalence relation defined by Object.equals()."
            }
        ],
        "correctOptionIds": ["opt_1", "opt_3", "opt_4"],
        "explanation": "The contract guarantees that $a.equals(b) \\implies a.hashCode() == b.hashCode()$, consistency of hashCode over time, and an equivalence relation for equals. The reverse is false: $a.hashCode() == b.hashCode()$ does not imply equality due to collisions.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Java Record Shallow Immutability",
        "slug": "java-record-shallow-immutability",
        "content": """In modern Java (Java 16+), components of a `record` are shallowly immutable: all state fields are implicitly declared `private final`.

However, if a record component references a mutable collection (e.g., `List<String>`), the internal contents of that collection can still be modified unless defensively copied.

Is this statement **True** or **False**?

```java
public record Team(String name, List<String> members) {}
```
""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Core Java"],
        "tags": ["records", "immutability", "java-16", "collections"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "Records guarantee shallow immutability (the reference cannot be rebound), but do not make referenced mutable objects deep-immutable automatically."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "Records do not magically deep-freeze mutable collections like ArrayList."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. Record component references are `final`. However, reference immutability is shallow. If a component is an `ArrayList`, callers can still call `members.add(...)`. A compact constructor with `List.copyOf(members)` is required for deep immutability.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Try-With-Resources Close Order",
        "slug": "try-with-resources-close-order",
        "content": """Consider the following resource declaration using Java's try-with-resources:

```java
class Res implements AutoCloseable {
    private final String id;
    Res(String id) { this.id = id; }
    public void close() { System.out.print(id + " "); }
}

public class Main {
    public static void main(String[] args) {
        try (Res r1 = new Res("A"); Res r2 = new Res("B")) {
            System.out.print("Body ");
        }
    }
}
```

What is printed to standard output?

> [!NOTE]
> Resources declared in the `try (...)` header implement `AutoCloseable`.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Core Java"],
        "tags": ["try-with-resources", "exceptions", "autocloseable", "resource-management"],
        "options": [
            {
                "id": "opt_a",
                "text": "`Body B A ` (Reverse of initialization order)",
                "explanation": "Java specifies that AutoCloseable resources are closed in the exact reverse order of their declaration."
            },
            {
                "id": "opt_b",
                "text": "`Body A B ` (Same as initialization order)",
                "explanation": "Closing in forward order could violate dependencies where r2 depends on r1."
            },
            {
                "id": "opt_c",
                "text": "`A B Body `",
                "explanation": "Resources are closed when leaving the try block, after the body finishes."
            },
            {
                "id": "opt_d",
                "text": "`B A Body `",
                "explanation": "Resources close upon exiting the try block."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "In Java try-with-resources, resources are closed in reverse order of declaration (LIFO stack discipline). This prevents dependent resources from having their underlying sources closed prematurely.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Volatile Modifier and the Java Memory Model",
        "slug": "volatile-modifier-and-jmm",
        "content": """Under the **Java Memory Model (JMM)**, which statements regarding a `volatile` variable are correct?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Core Java"],
        "tags": ["concurrency", "volatile", "jmm", "multithreading"],
        "options": [
            {
                "id": "opt_1",
                "text": "A write to a `volatile` variable *happens-before* every subsequent read of that same variable across threads",
                "explanation": "This establishes memory visibility across threads."
            },
            {
                "id": "opt_2",
                "text": "Compound operations such as increment (`count++`) are atomic on `volatile` variables",
                "explanation": "False. `count++` is a read-modify-write composite operation requiring AtomicInteger or locks for atomicity."
            },
            {
                "id": "opt_3",
                "text": "The compiler and CPU memory barriers prevent instruction reordering around volatile reads and writes",
                "explanation": "Volatile writes emit release barriers and volatile reads emit acquire barriers."
            },
            {
                "id": "opt_4",
                "text": "Reads and writes to 64-bit `long` and `double` variables declared `volatile` are guaranteed to be atomic",
                "explanation": "In JMM, standard 64-bit writes may be treated as two 32-bit writes unless declared volatile."
            }
        ],
        "correctOptionIds": ["opt_1", "opt_3", "opt_4"],
        "explanation": "A `volatile` variable provides visibility (happens-before relationship) and instruction reordering prevention. It also guarantees atomic reads/writes for 64-bit primitives (`long`/`double`). However, it does NOT make composite operations like `count++` atomic.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Garbage Collection Phases in G1 GC",
        "slug": "garbage-collection-phases-in-g1-gc",
        "content": """In the **Garbage-First (G1) Garbage Collector**:

Which phase of G1 GC is executed **concurrently** with application mutator threads without causing a global Stop-The-World (STW) pause?

> [!TIP]
> G1 partitions the heap into equal-sized regions and targets user-configured pause time goals.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Core Java", "Systems, Linux & Infrastructure"],
        "tags": ["jvm", "gc", "g1gc", "performance", "memory"],
        "options": [
            {
                "id": "opt_a",
                "text": "**Concurrent Marking**: Traversing the live object graph across the old generation while the application runs",
                "explanation": "The concurrent marking phase uses Snapshot-At-The-Beginning (SATB) write barriers to mark live objects concurrently without stopping application threads."
            },
            {
                "id": "opt_b",
                "text": "**Young Collection (Initial-Mark)**",
                "explanation": "Initial-mark piggybacks on a Stop-The-World young generation evacuation pause."
            },
            {
                "id": "opt_c",
                "text": "**Remark Phase**",
                "explanation": "The Remark phase is a Stop-The-World pause to empty and process pending SATB buffers."
            },
            {
                "id": "opt_d",
                "text": "**Cleanup / Evacuation Phase (Mixed Collection)**",
                "explanation": "Live object evacuation to copy survived objects into survivor/old regions is a Stop-The-World pause."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "In G1 GC, the concurrent marking phase runs concurrently with mutator threads in the background. Initial-mark, Remark, and Evacuation/Copying are Stop-The-World (STW) pauses.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "ConcurrentHashMap Thread-Safety Mechanism",
        "slug": "concurrenthashmap-thread-safety-mechanism",
        "content": """How does Java's modern `java.util.concurrent.ConcurrentHashMap` achieve high throughput for concurrent reads and writes compared to legacy `Hashtable`?

Select the most accurate architectural description:""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Core Java"],
        "tags": ["concurrency", "collections", "concurrenthashmap", "cas"],
        "options": [
            {
                "id": "opt_1",
                "text": "It uses lock-free volatile reads (`get()` requires no lock), CAS (Compare-And-Swap) for empty bucket insertion, and synchronizes only on the head node of a specific hash bin for updates",
                "explanation": "Modern ConcurrentHashMap locks only the head node of individual hash bins during conflicting writes, eliminating global table contention."
            },
            {
                "id": "opt_2",
                "text": "It locks the entire table using a single global `ReentrantReadWriteLock`",
                "explanation": "That would cause severe write contention across unrelated keys."
            },
            {
                "id": "opt_3",
                "text": "It clones the underlying array on every write (like `CopyOnWriteArrayList`)",
                "explanation": "CopyOnWrite is only used for read-heavy lists, not large concurrent hash tables."
            },
            {
                "id": "opt_4",
                "text": "It dispatches all operations to a single background event-loop thread",
                "explanation": "Java concurrent collections execute on the calling threads directly."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "Since Java 8, `ConcurrentHashMap` uses CAS for inserting the first node of a bucket, `synchronized` on the individual head node of the bucket when chaining or treeifying, and lock-free volatile reads.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Virtual Threads vs Platform Threads in Java 21+",
        "slug": "virtual-threads-vs-platform-threads-java21",
        "content": """Which statements accurately reflect the behavior and design of **Virtual Threads** (Project Loom, Java 21+)?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Core Java", "Backend Engineering & Spring"],
        "tags": ["virtual-threads", "java-21", "project-loom", "concurrency"],
        "options": [
            {
                "id": "opt_a",
                "text": "Virtual threads are lightweight user-mode threads managed by the JVM rather than 1:1 mapped OS kernel threads",
                "explanation": "Millions of virtual threads can run on a small pool of carrier OS platform threads."
            },
            {
                "id": "opt_b",
                "text": "When a virtual thread blocks on synchronous blocking I/O (e.g., `SocketInputStream.read()`), the JVM unmounts it from its carrier thread, allowing other virtual threads to execute",
                "explanation": "This allows high-throughput thread-per-request architectures without reactive callback pyramids."
            },
            {
                "id": "opt_c",
                "text": "CPU-bound mathematical computation benefits dramatically from running 100,000 virtual threads on a 4-core machine",
                "explanation": "Incorrect. Virtual threads excel at I/O-bound blocking tasks. CPU-bound throughput is bounded by physical hardware cores."
            },
            {
                "id": "opt_d",
                "text": "Thread pinning can occur when a virtual thread performs a blocking call inside a `synchronized` block or native method",
                "explanation": "Pinning prevents the virtual thread from unmounting from its carrier thread."
            }
        ],
        "correctOptionIds": ["opt_a", "opt_b", "opt_d"],
        "explanation": "Virtual threads are M:N scheduled user-mode threads that unmount during blocking I/O. They do not speed up CPU-bound computation and can be pinned to carrier threads when executing inside `synchronized` blocks or JNI calls.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Java Streams: Lazy Intermediate Operations",
        "slug": "java-streams-lazy-intermediate-operations",
        "content": """Examine the following Java Stream pipeline:

```java
List<String> items = List.of("apple", "banana", "cherry");

var stream = items.stream().filter(s -> {
    System.out.print(s + " ");
    return s.startsWith("b");
});

System.out.print("Start ");
long count = stream.count();
System.out.print("End");
```

What is printed to stdout?

> [!NOTE]
> Consider the execution lifecycle of intermediate vs terminal operations.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Core Java"],
        "tags": ["streams", "lambdas", "laziness", "functional"],
        "options": [
            {
                "id": "opt_1",
                "text": "`Start apple banana cherry End`",
                "explanation": "Stream intermediate operations like `.filter()` are lazy; no elements are processed until the terminal operation `.count()` is invoked after printing 'Start '."
            },
            {
                "id": "opt_2",
                "text": "`apple banana cherry Start End`",
                "explanation": "Stream pipelines are not eagerly evaluated upon declaration."
            },
            {
                "id": "opt_3",
                "text": "`Start banana End`",
                "explanation": "The filter predicate prints every examined element ('apple', 'banana', 'cherry'), not just matches."
            },
            {
                "id": "opt_4",
                "text": "`Start End`",
                "explanation": "The terminal operation count() triggers evaluation of all elements in the stream."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "Intermediate operations like `filter()` are completely lazy and do nothing until a terminal operation (`count()`) is invoked. Therefore, 'Start ' is printed first, followed by each element tested during stream traversal, followed by 'End'.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Checked vs Unchecked Exceptions",
        "slug": "checked-vs-unchecked-exceptions",
        "content": """In the Java exception hierarchy:

Which of the following classes is an **Unchecked Exception** (does NOT require explicit declaration in a `throws` clause or a `catch` block)?

```text
Throwable
├── Error
└── Exception
    ├── IOException (Checked)
    └── RuntimeException (Unchecked)
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Core Java", "Programming Fundamentals"],
        "tags": ["exceptions", "checked", "unchecked", "runtime-exception"],
        "options": [
            {
                "id": "opt_a",
                "text": "`java.lang.IllegalArgumentException`",
                "explanation": "IllegalArgumentException extends RuntimeException, making it an unchecked exception."
            },
            {
                "id": "opt_b",
                "text": "`java.io.IOException`",
                "explanation": "IOException extends Exception directly (not RuntimeException), making it checked."
            },
            {
                "id": "opt_c",
                "text": "`java.sql.SQLException`",
                "explanation": "SQLException is a checked exception extending Exception directly."
            },
            {
                "id": "opt_d",
                "text": "`java.lang.ClassNotFoundException`",
                "explanation": "ClassNotFoundException is a checked exception."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "In Java, subclasses of `RuntimeException` and `Error` are unchecked. `IllegalArgumentException` subclasses `RuntimeException`, so compiler enforcement of `try-catch` or `throws` does not apply.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Interface Default Method Conflict Resolution",
        "slug": "interface-default-method-conflict-resolution",
        "content": """If a class implements two interfaces `A` and `B`, and both interfaces provide identical `default void doWork()` method signatures with different implementations:

The implementing class **must** explicitly override `doWork()` to resolve the ambiguity, or the compiler will reject the code with a compilation error.

Is this statement **True** or **False**?

```java
interface A { default void doWork() { System.out.println("A"); } }
interface B { default void doWork() { System.out.println("B"); } }

// Does this compile without overriding doWork()?
class Service implements A, B {}
```
""",
        "type": "BOOLEAN",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Core Java"],
        "tags": ["interfaces", "default-methods", "diamond-problem", "oop"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "Java rules mandate that when two interfaces provide conflicting default implementations, the implementing class must explicitly override the method."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "The compiler will not guess or pick whichever interface was listed first."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. To prevent the classic diamond problem, Java requires the implementing class to explicitly override the conflicting default method and choose or provide an implementation (e.g., `A.super.doWork()`).",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Java Generics and Type Erasure",
        "slug": "java-generics-and-type-erasure",
        "content": """Analyze the following Java reflection snippet:

```java
List<String> stringList = new ArrayList<>();
List<Integer> intList = new ArrayList<>();

boolean sameClass = (stringList.getClass() == intList.getClass());
System.out.println(sameClass);
```

What is printed to stdout and why?

> [!NOTE]
> Consider the JVM runtime representation of generic collections.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Core Java"],
        "tags": ["generics", "type-erasure", "reflection", "jvm"],
        "options": [
            {
                "id": "opt_true",
                "text": "`true`: Java generics are implemented via **Type Erasure**; at runtime, both instances share the identical raw type `java.util.ArrayList`",
                "explanation": "The Java compiler strips type parameters during compilation for backward compatibility with pre-generics bytecode. Both instances have getClass() == ArrayList.class."
            },
            {
                "id": "opt_false",
                "text": "`false`: The JVM generates distinct reified classes for each type parameter specialization",
                "explanation": "C# and C++ reify generics/templates; standard Java does not create separate classes at runtime."
            },
            {
                "id": "opt_err",
                "text": "A compile-time error occurs because comparing generic classes is illegal",
                "explanation": "getClass() returns raw Class<?> and comparison is legal."
            },
            {
                "id": "opt_null",
                "text": "`false`: stringList is backed by char arrays while intList is backed by int arrays",
                "explanation": "ArrayList is backed by an Object[] array in both cases."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "Java generics use compile-time type erasure for backward compatibility. Type arguments (`<String>`, `<Integer>`) are checked at compile time and then erased to their upper bound (`Object`). Both objects share the exact same `ArrayList.class` at runtime.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Modern Java Sealed Classes and Pattern Matching",
        "slug": "modern-java-sealed-classes-pattern-matching",
        "content": """In modern Java (Java 17 / 21+):

```java
public sealed interface PaymentMethod permits CreditCard, BankTransfer, Crypto {}
public record CreditCard(String pan) implements PaymentMethod {}
public record BankTransfer(String iban) implements PaymentMethod {}
public record Crypto(String walletAddress) implements PaymentMethod {}
```

When evaluating a `PaymentMethod` instance in a `switch` expression that handles all three permitted subclasses:

A `default` branch is **not required**, because the compiler knows all permitted subtypes and guarantees exhaustive pattern matching at compile time.

Is this statement **True** or **False**?
""",
        "type": "BOOLEAN",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Core Java"],
        "tags": ["sealed-classes", "pattern-matching", "java-21", "switch-expressions"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "Sealed hierarchies allow the Java compiler to verify exhaustiveness without requiring a redundant default branch."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "Switch expressions over sealed types require no default clause when all permitted subtypes are covered."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. Sealed classes restrict which other classes may extend or implement them. Because the compiler has complete knowledge of all possible subclasses, pattern matching `switch` expressions over sealed types are checked for exhaustiveness, removing the need for a fallback `default` case.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    }
]
