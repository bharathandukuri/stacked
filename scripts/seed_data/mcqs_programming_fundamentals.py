"""Programming Fundamentals MCQs."""

QUESTIONS = [
    {
        "title": "Two's Complement Integer Overflow",
        "slug": "twos-complement-integer-overflow",
        "content": """Consider a standard 32-bit signed integer representation in modern systems (two's complement):

```java
int a = Integer.MAX_VALUE; // 2^31 - 1 = 2,147,483,647
int b = a + 1;
System.out.println(b);
```

What is the resulting value of `b` and why?

> [!NOTE]
> Assume standard IEEE / JVM wrap-around behavior without overflow traps.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals"],
        "tags": ["integers", "overflow", "twos-complement", "binary"],
        "options": [
            {
                "id": "opt_a",
                "text": "`-2147483648` ($ -2^{31} $) because the sign bit wraps to 1 in two's complement",
                "explanation": "In two's complement, adding 1 to 01111111_11111111_11111111_11111111 yields 10000000_00000000_00000000_00000000, which represents Integer.MIN_VALUE."
            },
            {
                "id": "opt_b",
                "text": "`2147483648` ($ 2^{31} $) because the runtime automatically expands integer precision",
                "explanation": "Standard 32-bit fixed width primitive integers do not automatically promote to 64-bit or BigInteger."
            },
            {
                "id": "opt_c",
                "text": "`0` because overflow always clears all bits to zero",
                "explanation": "Overflow does not zero out all bits; it follows standard binary modular arithmetic ($2^{32}$)."
            },
            {
                "id": "opt_d",
                "text": "An `ArithmeticException` is thrown at runtime",
                "explanation": "Java primitive integer addition does not throw exceptions on overflow; Math.addExact() would, but direct operator + does not."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "In 32-bit signed two's complement arithmetic, adding 1 to `0x7FFFFFFF` produces `0x80000000`, which evaluates to $-2^{31} = -2,147,483,648$.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Pass-By-Value vs Object References",
        "slug": "pass-by-value-vs-object-references",
        "content": """Analyze the following code snippet carefully:

```java
public class ParameterPass {
    static class Node { int val; Node(int v) { this.val = v; } }

    static void modify(Node n, int x) {
        x = 99;
        n.val = 42;
        n = new Node(100);
    }

    public static void main(String[] args) {
        Node node = new Node(10);
        int num = 5;
        modify(node, num);
        System.out.println(node.val + " " + num);
    }
}
```

What will be printed to stdout?

| Variable | Scope | Reassigned in method? |
| :--- | :--- | :--- |
| `num` | Primitive | Yes (local copy) |
| `node` | Reference | Reference reassigned locally; internal field mutated |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals", "Core Java"],
        "tags": ["pass-by-value", "memory", "reference-semantics"],
        "options": [
            {
                "id": "opt_1",
                "text": "`42 5`",
                "explanation": "The reference `node` is passed by value (copy of reference). Mutating `n.val` alters the referenced object on the heap, but reassigning `n = new Node(100)` only modifies the local parameter copy."
            },
            {
                "id": "opt_2",
                "text": "`100 5`",
                "explanation": "Reassigning parameter `n` does not change the caller's reference `node`."
            },
            {
                "id": "opt_3",
                "text": "`100 99`",
                "explanation": "Primitives in Java are passed strictly by value; changing `x` has zero effect on `num`."
            },
            {
                "id": "opt_4",
                "text": "`42 99`",
                "explanation": "Primitive `num` remains unchanged in caller's frame."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "Java is strictly pass-by-value. For primitives (`num`), a copy of the bits is passed. For objects (`node`), a copy of the reference pointer is passed. Mutating through `n.val = 42` mutates the underlying heap object, but reassigning `n = new Node(...)` only rebinds the local reference.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Short-Circuit Logical Evaluation",
        "slug": "short-circuit-logical-evaluation",
        "content": """In modern high-level programming languages (e.g., Java, C++, JavaScript), the logical AND (`&&`) operator guarantees **short-circuit evaluation**: if the left-hand operand evaluates to `false`, the right-hand operand is never evaluated.

Is this statement **True** or **False**?

```java
// Idiomatic defensive null check relying on short-circuit:
if (user != null && user.isActive()) {
    process(user);
}
```
""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals"],
        "tags": ["operators", "short-circuit", "boolean-logic"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "Short-circuiting ensures the second operand is skipped if the first determines the outcome."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "Both operands are always evaluated in bitwise operators (&, |), but not logical operators (&&, ||)."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. The `&&` operator evaluates the left operand first; if `false`, the overall expression cannot be `true`, so the right side is skipped. This is essential for guarding against `NullPointerException` or division by zero.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Recursion vs Iteration and Stack Overhead",
        "slug": "recursion-vs-iteration-stack-overhead",
        "content": """Which of the following statements regarding **recursion and stack frames** are accurate?

Select all that apply.

> [!IMPORTANT]
> Consider typical runtime environments like standard JVM or V8.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals", "Data Structures & Algorithms"],
        "tags": ["recursion", "call-stack", "stack-overflow", "tail-recursion"],
        "options": [
            {
                "id": "opt_a",
                "text": "Each non-inlined recursive call allocates a new stack frame containing local variables and return address",
                "explanation": "Correct. Function invocations push activation records onto the call stack."
            },
            {
                "id": "opt_b",
                "text": "Tail-call optimization (TCO) allows a compiler/runtime to reuse the current stack frame if the recursive call is in tail position",
                "explanation": "Correct. When the final action of a function is returning the result of a recursive call, no caller state needs to be retained."
            },
            {
                "id": "opt_c",
                "text": "Standard HotSpot Java Virtual Machine (Java 21) automatically optimizes all tail-recursive methods to iterative loops",
                "explanation": "Incorrect. Standard HotSpot JVM does NOT perform automatic tail-call optimization for security and stack inspection reasons."
            },
            {
                "id": "opt_d",
                "text": "Unbounded recursion without a proper base condition results in a `StackOverflowError`",
                "explanation": "Correct. Exceeding thread stack limits exhausts stack memory."
            }
        ],
        "correctOptionIds": ["opt_a", "opt_b", "opt_d"],
        "explanation": "Statements A, B, and D are correct. HotSpot JVM notably does NOT support general tail-call optimization, meaning tail recursion in standard Java still consumes stack frames.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Pure Functions and Side Effects",
        "slug": "pure-functions-and-side-effects",
        "content": """Which characteristics are mandatory for a function to be classified as a **pure function** in functional programming?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals"],
        "tags": ["functional-programming", "pure-functions", "immutability"],
        "options": [
            {
                "id": "opt_1",
                "text": "Given the identical arguments, it always returns the exact same result (referential transparency)",
                "explanation": "A pure function is deterministic and does not rely on mutable hidden state."
            },
            {
                "id": "opt_2",
                "text": "Its execution causes no observable side effects (e.g., mutating external state, writing to disk/network, throwing unhandled exceptions)",
                "explanation": "Pure functions do not alter state outside their local scope."
            },
            {
                "id": "opt_3",
                "text": "It must be declared with asynchronous or reactive stream primitives",
                "explanation": "Purity has nothing to do with asynchronous or reactive wrappers; sync functions can be pure."
            },
            {
                "id": "opt_4",
                "text": "It can safely be memoized or executed concurrently across threads without synchronization locks",
                "explanation": "Because pure functions are deterministic and stateless, caching results and concurrent execution are inherently safe."
            }
        ],
        "correctOptionIds": ["opt_1", "opt_2", "opt_4"],
        "explanation": "A pure function has referential transparency (same input $\\to$ same output), causes no side effects, and enables thread safety and memoization.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Floating-Point Precision in IEEE 754",
        "slug": "floating-point-precision-ieee754",
        "content": """In languages adhering to **IEEE 754 floating-point arithmetic** (e.g., Java, JavaScript, Python):

```python
result = 0.1 + 0.2 == 0.3
print(result)
```

Why does this expression evaluate to `False`?

> [!CAUTION]
> Common trap for currency and high-precision calculations.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals"],
        "tags": ["floating-point", "ieee-754", "precision", "math"],
        "options": [
            {
                "id": "opt_a",
                "text": "Decimal fractions like 0.1 and 0.2 cannot be represented with exact finite precision in base-2 binary floating point, causing round-off discrepancy",
                "explanation": "In binary, $0.1_{10} = 0.0001100110011..._2$ is an infinitely repeating binary fraction. When truncated to 53 bits of mantissa, the sum equals approximately 0.30000000000000004."
            },
            {
                "id": "opt_b",
                "text": "The addition operator overflows the exponent bits of double precision",
                "explanation": "The numbers 0.1 and 0.2 are far from overflowing the double-precision exponent range."
            },
            {
                "id": "opt_c",
                "text": "Python casts floats to integers before performing equality comparisons",
                "explanation": "Python performs float-to-float equality checks without casting to integers."
            },
            {
                "id": "opt_d",
                "text": "The equality operator `==` checks object memory identity rather than value equality",
                "explanation": "In Python, `==` tests value equality, while `is` tests identity."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "Just as $1/3$ cannot be represented finitely in decimal base-10, $0.1$ and $0.2$ are infinitely repeating binary fractions in base-2. Adding them results in `0.30000000000000004`, which is not equal to `0.3`.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Bitwise Manipulation: Lowest Set Bit Clearing",
        "slug": "bitwise-manipulation-lowest-set-bit",
        "content": """What does the expression `x & (x - 1)` achieve for any non-zero integer `x`?

```text
Example: x = 12 (binary 1100)
         x - 1 = 11 (binary 1011)
         x & (x - 1) = 1100 & 1011 = 1000 (binary 8)
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals", "Data Structures & Algorithms"],
        "tags": ["bitwise", "algorithms", "tricks"],
        "options": [
            {
                "id": "opt_1",
                "text": "Clears the lowest (rightmost) set bit (1-bit) of `x`",
                "explanation": "Subtracting 1 flips the rightmost set bit and all subsequent trailing zeros. Bitwise ANDing with original x clears exactly that lowest set bit."
            },
            {
                "id": "opt_2",
                "text": "Multiplies `x` by 2 by shifting bits left",
                "explanation": "Shifting left `x << 1` multiplies by 2."
            },
            {
                "id": "opt_3",
                "text": "Inverts all bits of `x` to compute two's complement",
                "explanation": "Bitwise NOT `~x` inverts bits."
            },
            {
                "id": "opt_4",
                "text": "Extracts only the highest set bit of `x`",
                "explanation": "It clears the lowest bit, leaving other bits intact."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "`x & (x - 1)` drops the lowest set bit. This trick is famously used in Brian Kernighan's algorithm to count set bits in $O(k)$ time where $k$ is the number of 1s, and to check if a number is a power of 2 via `(x > 0) && ((x & (x - 1)) == 0)`.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Refactoring Code Smells",
        "slug": "refactoring-code-smells",
        "content": """A class named `OrderProcessor` contains 4,500 lines of code. It directly connects to the database, sends email confirmations, parses credit card XML payloads, generates PDF invoices, and logs metrics to Datadog.

Which refactoring code smells are exhibited here?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals", "Software Architecture & LLD"],
        "tags": ["code-smell", "refactoring", "clean-code", "solid"],
        "options": [
            {
                "id": "opt_god_class",
                "text": "**God Class (Large Class)**: Violating the Single Responsibility Principle by accumulating too many unrelated responsibilities",
                "explanation": "The class manages orders, email, PDFs, database, and telemetry all in one place."
            },
            {
                "id": "opt_tight_coupling",
                "text": "**Tight Coupling**: High interdependency between payment parsing, data storage, and external communication channels",
                "explanation": "Changes to invoice format or payment gateways force changes inside the order processor."
            },
            {
                "id": "opt_low_cohesion",
                "text": "**Low Cohesion**: Methods and fields within the class do not share a focused, singular conceptual purpose",
                "explanation": "PDF rendering code shares almost no logical relation with database query logic."
            },
            {
                "id": "opt_dead_code",
                "text": "**Dead Code**: Unreachable code blocks that the compiler automatically deletes",
                "explanation": "The description indicates active code with too many responsibilities, not dead code."
            }
        ],
        "correctOptionIds": ["opt_god_class", "opt_tight_coupling", "opt_low_cohesion"],
        "explanation": "The class is a classic 'God Class' suffering from low cohesion and high coupling, violating the Single Responsibility Principle. Extracting specialized delegates (e.g., `PaymentService`, `InvoiceGenerator`, `NotificationService`) resolves these smells.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Concurrent Modification During Collection Iteration",
        "slug": "concurrent-modification-collection-iteration",
        "content": """Analyze the following Java code:

```java
List<String> items = new ArrayList<>(List.of("alpha", "beta", "gamma"));

for (String s : items) {
    if (s.equals("beta")) {
        items.remove(s); // Mutating list during enhanced for-loop
    }
}
```

What occurs when this loop executes?

> [!WARNING]
> Enhanced for-loops in Java compile to underlying `Iterator` instances.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals", "Core Java"],
        "tags": ["collections", "iterator", "concurrent-modification", "fail-fast"],
        "options": [
            {
                "id": "opt_cme",
                "text": "A `java.util.ConcurrentModificationException` is thrown because the underlying iterator detects an unexpected `modCount` change",
                "explanation": "Java ArrayList iterators are fail-fast. Calling list.remove() modifies modCount without updating expectedModCount in the iterator, failing on the next iterator.next() check."
            },
            {
                "id": "opt_ok",
                "text": "The element 'beta' is cleanly removed and the list contains ['alpha', 'gamma']",
                "explanation": "Direct list mutation during enhanced for loops throws ConcurrentModificationException."
            },
            {
                "id": "opt_skip",
                "text": "The loop terminates cleanly but silently skips 'gamma'",
                "explanation": "While index shifting occurs, the fail-fast check triggers an exception before completing."
            },
            {
                "id": "opt_null",
                "text": "The element at 'beta' is replaced by null",
                "explanation": "remove() shrinks the collection rather than inserting null."
            }
        ],
        "correctOptionIds": ["opt_cme"],
        "explanation": "Java's collection iterators are fail-fast. When `items.remove(s)` is called directly on the `List` instead of through `Iterator.remove()`, the list's internal `modCount` diverges from the iterator's `expectedModCount`, causing `ConcurrentModificationException` on the next iteration.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Lexical Scope and Variable Shadowing",
        "slug": "lexical-scope-and-variable-shadowing",
        "content": """In languages supporting block scope and variable shadowing:

When an inner block declares a variable with the identical identifier as a variable in an enclosing outer scope:

The inner variable **shadows** the outer variable within that block; references to that identifier inside the block resolve to the inner variable without destroying the outer variable's value outside the block.

Is this statement **True** or **False**?

```javascript
let count = 10;
{
    let count = 20; // Shadows outer count
    console.log(count); // 20
}
console.log(count); // 10
```
""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Programming Fundamentals"],
        "tags": ["scoping", "shadowing", "lexical-scope", "variables"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "Lexical scoping rules resolve names starting from the innermost scope outwards. Shadowing overrides the identifier within the block while leaving the outer binding intact."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "Shadowing does not destroy or overwrite the outer binding once the inner block exits."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. Variable shadowing occurs when a variable declared within a local scope has the same name as a variable in an outer scope. The inner declaration takes precedence during identifier resolution within that block.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    }
]
