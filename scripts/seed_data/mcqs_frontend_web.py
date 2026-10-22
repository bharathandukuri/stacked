"""Frontend and Web Development MCQs."""

QUESTIONS = [
    {
        "title": "JavaScript Event Loop Execution Order",
        "slug": "javascript-event-loop-execution-order",
        "content": """Analyze the following JavaScript code snippet:

```javascript
console.log("1");

setTimeout(() => {
    console.log("2");
}, 0);

Promise.resolve().then(() => {
    console.log("3");
}).then(() => {
    console.log("4");
});

console.log("5");
```

In which order will the numbers be logged to the console?

> [!NOTE]
> Consider Call Stack (synchronous), Microtask Queue (Promises), and Macrotask Queue (Timers).""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Frontend & Web Development"],
        "tags": ["javascript", "event-loop", "promises", "async"],
        "options": [
            {
                "id": "opt_a",
                "text": "`1, 5, 3, 4, 2`",
                "explanation": "Synchronous script runs first ('1', '5'). Microtask queue is drained before any macrotasks ('3', '4'). Finally the macrotask timer callback runs ('2')."
            },
            {
                "id": "opt_b",
                "text": "`1, 2, 3, 4, 5`",
                "explanation": "setTimeout is asynchronous; it does not run synchronously."
            },
            {
                "id": "opt_c",
                "text": "`1, 5, 2, 3, 4`",
                "explanation": "Microtasks (Promises) always take precedence over Macrotasks (setTimeout)."
            },
            {
                "id": "opt_d",
                "text": "`1, 3, 5, 4, 2`",
                "explanation": "Synchronous code ('5') finishes before microtask queue processing starts."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "The JavaScript event loop executes all synchronous code in the call stack first (`1`, `5`). Then it drains all pending jobs in the Microtask queue (`3`, `4`). Only when the microtask queue is empty does the event loop dequeue from the Macrotask (Timer) queue (`2`). Result: `1, 5, 3, 4, 2`.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "JavaScript Closures in Asynchronous Loops",
        "slug": "javascript-closures-in-asynchronous-loops",
        "content": """Consider the classic JavaScript loop behavior:

```javascript
for (var i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 100);
}

for (let j = 0; j < 3; j++) {
    setTimeout(() => console.log(j), 100);
}
```

What is printed to the console when the timers fire?

| Loop variable | Scope | Binding behavior |
| :--- | :--- | :--- |
| `var i` | Function / Global | Single shared mutable binding across iterations |
| `let j` | Block scope | Distinct lexical binding created per iteration |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Frontend & Web Development", "Programming Fundamentals"],
        "tags": ["javascript", "closures", "scope", "hoisting"],
        "options": [
            {
                "id": "opt_1",
                "text": "First loop prints `3 3 3`; Second loop prints `0 1 2`",
                "explanation": "With `var`, i is function-scoped; all three callbacks close over the same shared variable whose final value is 3. With `let`, each loop iteration creates a new block-scoped lexical environment binding the value of j for that iteration."
            },
            {
                "id": "opt_2",
                "text": "Both loops print `0 1 2`",
                "explanation": "var does not create block scope; it shares a single binding."
            },
            {
                "id": "opt_3",
                "text": "Both loops print `3 3 3`",
                "explanation": "let creates per-iteration bindings, so j does not overwrite previous iterations."
            },
            {
                "id": "opt_4",
                "text": "First loop prints `0 1 2`; Second loop prints `3 3 3`",
                "explanation": "Inverts the scoping behavior of var and let."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "`var` is function-scoped, so all callbacks share a single `i` whose final post-loop value is `3`. `let` is block-scoped and re-bound for every iteration of the `for` loop, preserving `0`, `1`, `2` in each respective closure.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "React Keys and Fiber Reconciliation",
        "slug": "react-keys-and-fiber-reconciliation",
        "content": """In React (18/19), why is using array index as a `key` (e.g. `key={index}`) strongly discouraged for dynamic lists whose items can be reordered, inserted, or deleted?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Frontend & Web Development"],
        "tags": ["react", "reconciliation", "fiber", "performance"],
        "options": [
            {
                "id": "opt_state",
                "text": "Component internal state (such as uncontrolled inputs, form state, or local component state) can become bound to the wrong item when the list is reordered",
                "explanation": "React matches DOM nodes and Fiber state based on key; reordering items with index keys causes Fiber nodes to retain prior state for new items."
            },
            {
                "id": "opt_perf",
                "text": "Re-rendering performance suffers because inserting an item at the beginning changes the index key of all subsequent items, forcing unneeded reconciliation",
                "explanation": "Prepending an item alters index 0 -> 1 -> 2..., making React think every element changed."
            },
            {
                "id": "opt_error",
                "text": "React throws a fatal runtime `TypeError` when an integer index key is detected",
                "explanation": "False. React warns in development console, but does not throw an exception."
            },
            {
                "id": "opt_animations",
                "text": "CSS layout transitions and animations break because DOM identity is not preserved across item movement",
                "explanation": "Keys determine DOM reuse; index keys cause DOM content replacement rather than physical node movement."
            }
        ],
        "correctOptionIds": ["opt_state", "opt_perf", "opt_animations"],
        "explanation": "Using array indexes as keys causes component state pollution on deletions/reorders, destroys DOM transition continuity, and degrades reconciliation performance. Stable, unique IDs (e.g. `item.id`) must be used instead.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "TypeScript: unknown vs any Type Safety",
        "slug": "typescript-unknown-vs-any-type-safety",
        "content": """In TypeScript, the `unknown` type is the type-safe counterpart of `any`.

While any value can be assigned to `unknown`, you **cannot** perform operations on, call methods on, or access properties of an `unknown` variable without first narrowing its type via type guards or assertions.

Is this statement **True** or **False**?

```typescript
let val: unknown = "stacked";

// Compiler Error: 'val' is of type 'unknown'
// val.toUpperCase();

// Valid narrowing:
if (typeof val === "string") {
    console.log(val.toUpperCase()); // Allowed!
}
```
""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Frontend & Web Development"],
        "tags": ["typescript", "types", "type-safety", "narrowing"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "`unknown` forces explicit type checking/narrowing before any property access or invocation, whereas `any` completely disables compiler type checks."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "`unknown` is strictly designed as a type-safe top type."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. `unknown` represents any value, but TypeScript prevents invoking methods or reading properties until you narrow the type using `typeof`, `instanceof`, or custom type guards.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "React useEffect Stale Closure Bug",
        "slug": "react-useeffect-stale-closure-bug",
        "content": """Examine this React component:

```tsx
function Timer() {
    const [count, setCount] = useState(0);

    useEffect(() => {
        const id = setInterval(() => {
            setCount(count + 1); // Notice: uses count directly
        }, 1000);
        return () => clearInterval(id);
    }, []); // Empty dependency array

    return <div>{count}</div>;
}
```

What will be displayed in the UI after 5 seconds, and what is the underlying cause?

> [!CAUTION]
> Common React interview question regarding hooks and closures.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Frontend & Web Development"],
        "tags": ["react", "hooks", "closures", "stale-state"],
        "options": [
            {
                "id": "opt_a",
                "text": "It displays `1` permanently; the interval callback closes over the initial `count = 0` at mount and repeatedly evaluates `setCount(0 + 1)`",
                "explanation": "Because the dependency array is empty, the effect never re-runs, and its closure always references the initial render's count variable (0). Using the functional updater `setCount(prev => prev + 1)` fixes this bug."
            },
            {
                "id": "opt_b",
                "text": "It displays `5`; React automatically reads the latest state across all closures",
                "explanation": "JavaScript closures capture variable values in scope at definition time, not dynamic references."
            },
            {
                "id": "opt_c",
                "text": "It throws an `InfiniteRenderException`",
                "explanation": "No infinite render occurs because setInterval fires once per second."
            },
            {
                "id": "opt_d",
                "text": "It displays `0` because setInterval is garbage collected immediately",
                "explanation": "The cleanup function only runs on unmount; the interval continues firing."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "This is a classic 'stale closure' bug. The effect closure captured `count = 0` from the initial render. Every second, it executes `setCount(0 + 1)`, forever setting state to `1`. The correct idiom is the updater form: `setCount(c => c + 1)`.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "TypeScript Discriminated Unions and Exhaustiveness Check",
        "slug": "typescript-discriminated-unions-exhaustiveness",
        "content": """Consider the following TypeScript pattern:

```typescript
type Action = 
  | { type: "FETCH_START" }
  | { type: "FETCH_SUCCESS"; payload: string }
  | { type: "FETCH_ERROR"; error: Error };

function handleAction(action: Action) {
    switch (action.type) {
        case "FETCH_START": return "loading";
        case "FETCH_SUCCESS": return action.payload;
        case "FETCH_ERROR": return action.error.message;
        default:
            const _exhaustiveCheck: never = action;
            return _exhaustiveCheck;
    }
}
```

What is the architectural purpose of assigning `action` to a variable of type `never` in the `default` branch?

Select the most accurate explanation:""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Frontend & Web Development"],
        "tags": ["typescript", "discriminated-unions", "type-safety", "exhaustiveness"],
        "options": [
            {
                "id": "opt_1",
                "text": "**Compile-time exhaustiveness checking**: If a developer adds a new variant to `Action` (e.g. `{ type: 'CANCEL' }`) without handling it in the `switch`, TypeScript will raise a compile-time type error",
                "explanation": "Since `never` cannot be assigned any non-never value, any unhandled union member falling into `default` triggers a compiler failure, preventing unhandled cases."
            },
            {
                "id": "opt_2",
                "text": "It tells the JavaScript V8 engine to optimize the switch statement into a fast binary jump table",
                "explanation": "TypeScript types are erased at compile time; `never` has zero runtime representation."
            },
            {
                "id": "opt_3",
                "text": "It acts as a memory cleanup garbage collection hint",
                "explanation": "Type annotations do not affect garbage collection."
            },
            {
                "id": "opt_4",
                "text": "It prevents throwing runtime exceptions when action is null",
                "explanation": "If action is null at runtime, it does not prevent runtime errors."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "The `never` pattern provides compile-time exhaustiveness checking. If a new member is added to the union type, TypeScript's control flow analysis deduces that `action` in the `default` case is that new type, failing the assignment to `never`.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "CSS Specificity Calculation Hierarchy",
        "slug": "css-specificity-calculation-hierarchy",
        "content": """Given the following HTML element:

```html
<button id="submit-btn" class="btn primary" style="color: blue;">Submit</button>
```

And CSS rules:

```css
#submit-btn { color: green; }
button.btn.primary { color: red !important; }
.btn { color: orange; }
```

What text color will the button render in the browser and why?

| Rule | Specificity Vector | `!important`? |
| :--- | :--- | :--- |
| `button.btn.primary` | `(0, 2, 1)` | Yes |
| `style="color: blue;"` | Inline `(1, 0, 0, 0)` | No |
| `#submit-btn` | ID `(0, 1, 0, 0)` | No |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Frontend & Web Development"],
        "tags": ["css", "specificity", "styling", "cascade"],
        "options": [
            {
                "id": "opt_red",
                "text": "**Red**: The `!important` flag overrides normal cascading and specificity rules (including inline styles and ID selectors)",
                "explanation": "Declarations with !important supersede normal declarations, including inline styles without !important."
            },
            {
                "id": "opt_blue",
                "text": "**Blue**: Inline styles always take precedence over external stylesheets regardless of `!important`",
                "explanation": "Inline styles only override external CSS if the external rule does NOT have !important."
            },
            {
                "id": "opt_green",
                "text": "**Green**: ID selectors have the highest specificity in CSS",
                "explanation": "ID selectors are overridden by inline styles and !important flags."
            },
            {
                "id": "opt_orange",
                "text": "**Orange**: The rule defined last in the stylesheet takes precedence",
                "explanation": "Source order is only a tie-breaker when specificity and importance are equal."
            }
        ],
        "correctOptionIds": ["opt_red"],
        "explanation": "In the CSS Cascade, origin and importance (`!important`) supersede selector specificity. Even though inline styles normally beat class selectors, an author `!important` declaration (`color: red !important`) overrides normal inline styles (`color: blue`).",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "CORS Preflight OPTIONS Request Trigger Conditions",
        "slug": "cors-preflight-options-trigger-conditions",
        "content": """When a browser web application executes a cross-origin `fetch()` or `axios` call:

Which conditions will trigger an automatic browser **CORS Preflight `OPTIONS` request** before the actual HTTP request is sent?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Frontend & Web Development", "Backend Engineering & Spring"],
        "tags": ["cors", "browser", "security", "http", "preflight"],
        "options": [
            {
                "id": "opt_json",
                "text": "The request sets `Content-Type: application/json`",
                "explanation": "application/json is not a 'simple' Content-Type (only application/x-www-form-urlencoded, multipart/form-data, and text/plain are simple)."
            },
            {
                "id": "opt_auth",
                "text": "The request includes custom HTTP headers such as `Authorization: Bearer <token>` or `X-Custom-Header`",
                "explanation": "Any header outside the CORS-safelisted list triggers preflight."
            },
            {
                "id": "opt_method",
                "text": "The request uses HTTP methods like `PUT`, `DELETE`, or `PATCH`",
                "explanation": "Only GET, HEAD, and POST can be simple requests without preflight."
            },
            {
                "id": "opt_get",
                "text": "A standard `GET` request with no custom headers fetching a public image",
                "explanation": "Simple GET requests without custom headers do not trigger preflight OPTIONS."
            }
        ],
        "correctOptionIds": ["opt_json", "opt_auth", "opt_method"],
        "explanation": "A CORS request triggers a preflight `OPTIONS` check if it uses methods other than `GET`/`POST`/`HEAD`, sets custom headers (like `Authorization`), or uses a `Content-Type` other than the three safelisted formats (`text/plain`, `multipart/form-data`, `application/x-www-form-urlencoded`).",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    }
]
