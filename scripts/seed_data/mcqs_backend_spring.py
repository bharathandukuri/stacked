"""Backend Engineering and Spring Boot MCQs."""

QUESTIONS = [
    {
        "title": "HTTP Status Codes: 401 Unauthorized vs 403 Forbidden",
        "slug": "http-status-codes-401-vs-403",
        "content": """What is the precise RFC 9110 distinction between HTTP status codes **`401 Unauthorized`** and **`403 Forbidden`**?

| Status Code | Client State | Required Server Header |
| :--- | :--- | :--- |
| `401 Unauthorized` | Missing or invalid authentication credentials | `WWW-Authenticate` |
| `403 Forbidden` | Authenticated client lacks permission/privilege | None |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Backend Engineering & Spring"],
        "tags": ["http", "rest", "status-codes", "security"],
        "options": [
            {
                "id": "opt_a",
                "text": "**401** means authentication is required or failed (user identity unknown); **403** means the user is authenticated, but authorization is denied (user identity is known, but lacks necessary role/permission)",
                "explanation": "401 indicates missing or bad credentials (should prompt login). 403 indicates authorization failure (e.g. regular user attempting admin endpoint)."
            },
            {
                "id": "opt_b",
                "text": "**401** is only returned for expired JWT tokens, while **403** is returned for invalid passwords",
                "explanation": "Invalid passwords also result in 401."
            },
            {
                "id": "opt_c",
                "text": "**401** indicates a client error, while **403** indicates a backend database connection timeout",
                "explanation": "Both 401 and 403 are 4xx client errors."
            },
            {
                "id": "opt_d",
                "text": "**401** and **403** can be used interchangeably according to RFC 9110 specifications",
                "explanation": "RFC 9110 explicitly distinguishes between authentication failure (401) and authorization refusal (403)."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "`401 Unauthorized` means the request lacks valid authentication credentials (identity unverified). `403 Forbidden` means the server understood the credentials, but the authenticated principal lacks authorization to access the target resource.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "REST API Idempotency Specification",
        "slug": "rest-api-idempotency-specification",
        "content": """An HTTP method is considered **idempotent** if the intended effect on the server of multiple identical requests is the same as the effect of a single request.

According to RFC 9110, which of the following HTTP methods are defined as **idempotent**?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Backend Engineering & Spring"],
        "tags": ["rest", "http", "idempotency", "api-design"],
        "options": [
            {
                "id": "opt_get",
                "text": "`GET`: Safe and idempotent read operations",
                "explanation": "GET retrieves representations without modifying resource state."
            },
            {
                "id": "opt_put",
                "text": "`PUT`: Replaces the target resource with the request payload",
                "explanation": "Calling PUT multiple times with the same state produces the identical final resource state."
            },
            {
                "id": "opt_delete",
                "text": "`DELETE`: Deletes the target resource",
                "explanation": "Deleting an existing resource once leaves it deleted; repeated calls result in 404/204 but the resource remains deleted."
            },
            {
                "id": "opt_post",
                "text": "`POST`: Submits an entity causing server-side side-effects or creation",
                "explanation": "Calling POST repeatedly creates multiple resources (non-idempotent)."
            }
        ],
        "correctOptionIds": ["opt_get", "opt_put", "opt_delete"],
        "explanation": "`GET`, `HEAD`, `PUT`, `DELETE`, `OPTIONS`, and `TRACE` are idempotent. `POST` and `PATCH` are non-idempotent by default (though PATCH can be implemented idempotently).",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Spring Dependency Injection: Prototype in Singleton Pitfall",
        "slug": "spring-di-prototype-in-singleton-pitfall",
        "content": """In a Spring Boot application:

```java
@Scope("prototype")
@Component
public class TaskTokenGenerator { ... }

@Service
public class OrderService {
    @Autowired
    private TaskTokenGenerator tokenGenerator;
}
```

What happens when multiple concurrent threads call `orderService.process()`?

> [!CAUTION]
> Notice the lifecycle scope difference between the service and the dependency.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Backend Engineering & Spring", "Core Java"],
        "tags": ["spring", "spring-boot", "dependency-injection", "bean-scopes"],
        "options": [
            {
                "id": "opt_1",
                "text": "The prototype bean `TaskTokenGenerator` is injected only ONCE when the singleton `OrderService` is instantiated at startup, meaning all subsequent requests share the identical token generator instance",
                "explanation": "Because OrderService is a singleton initialized once, its dependencies are resolved once during initialization. To get a new prototype instance per request, use ObjectProvider<TaskTokenGenerator>, a scoped proxy, or @Lookup."
            },
            {
                "id": "opt_2",
                "text": "Spring dynamically creates a new `TaskTokenGenerator` instance on every method invocation on `OrderService`",
                "explanation": "Standard field injection does not create dynamic proxies unless @Scope(proxyMode = ...) is configured."
            },
            {
                "id": "opt_3",
                "text": "Spring Boot fails to start with a `BeanCreationException` due to scope mismatch",
                "explanation": "Spring allows injecting prototype beans into singleton beans without startup errors."
            },
            {
                "id": "opt_4",
                "text": "A new `OrderService` is created for every thread",
                "explanation": "OrderService remains a standard singleton."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "Injecting a prototype bean into a singleton bean means the prototype is instantiated only once during singleton initialization. To obtain a fresh prototype on every call, use `ObjectProvider<T>`, `@Lookup`, or define the prototype with `proxyMode = ScopedProxyMode.TARGET_CLASS`.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Spring @Transactional Self-Invocation and Rollback Rules",
        "slug": "spring-transactional-self-invocation-rollback",
        "content": """Which statements regarding Spring's proxy-based `@Transactional` are correct?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Backend Engineering & Spring"],
        "tags": ["spring", "transactions", "aop", "proxies"],
        "options": [
            {
                "id": "opt_proxy",
                "text": "Calling a `@Transactional` method from another method within the same class (`this.method()`) bypasses the Spring AOP proxy, resulting in no transaction being started",
                "explanation": "Internal self-invocation calls the method on the target instance directly without going through the surrounding transactional advice."
            },
            {
                "id": "opt_rollback",
                "text": "By default, `@Transactional` automatically rolls back for unchecked exceptions (`RuntimeException` and `Error`), but does NOT roll back for checked exceptions (`Exception`) unless `rollbackFor = Exception.class` is specified",
                "explanation": "Default Spring transaction rollback policy only triggers on unchecked exceptions."
            },
            {
                "id": "opt_private",
                "text": "Annotating `private` methods with `@Transactional` works seamlessly with standard Spring CGLIB/JDK dynamic proxies",
                "explanation": "Spring AOP proxies can only intercept public method calls. Annotations on private methods are silently ignored."
            },
            {
                "id": "opt_propagation",
                "text": "`Propagation.REQUIRES_NEW` suspends any existing active transaction and opens an independent physical transaction",
                "explanation": "REQUIRES_NEW isolates execution inside a fresh separate database connection/transaction."
            }
        ],
        "correctOptionIds": ["opt_proxy", "opt_rollback", "opt_propagation"],
        "explanation": "Spring transactions use AOP proxies. Direct `this.method()` calls bypass the proxy. By default, rollback occurs only for unchecked exceptions. Annotating `private` methods has no effect in standard Spring AOP proxies.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Circuit Breaker State Machine",
        "slug": "circuit-breaker-state-machine",
        "content": """In distributed system resilience libraries (such as Resilience4j):

Match each **Circuit Breaker** state to its operational behavior:

| State | Request Handling Behavior |
| :--- | :--- |
| **CLOSED** | ? |
| **OPEN** | ? |
| **HALF-OPEN** | ? |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Backend Engineering & Spring", "Software Architecture & LLD"],
        "tags": ["resilience", "circuit-breaker", "microservices", "resilience4j"],
        "options": [
            {
                "id": "opt_1",
                "text": "**CLOSED**: Normal operation, requests routed to remote service | **OPEN**: Remote service is failing, requests fail immediately (fast-fail) without calling remote service | **HALF-OPEN**: Trial period, permits a limited number of test requests to verify if remote service has recovered",
                "explanation": "Standard electrical circuit analogy: CLOSED allows current to flow; OPEN breaks the circuit; HALF-OPEN probes recovery."
            },
            {
                "id": "opt_2",
                "text": "**CLOSED**: Service blocked | **OPEN**: Service operating normally | **HALF-OPEN**: Requests queued in database",
                "explanation": "Inverts the meaning of CLOSED and OPEN."
            },
            {
                "id": "opt_3",
                "text": "**CLOSED**: Half requests fail | **OPEN**: All requests succeed | **HALF-OPEN**: Random 50% split",
                "explanation": "Incorrect definition."
            },
            {
                "id": "opt_4",
                "text": "**CLOSED**: Retries enabled | **OPEN**: Circuit disabled | **HALF-OPEN**: Fallback executing",
                "explanation": "Incorrect states."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "When healthy, the circuit is `CLOSED`. When error thresholds are exceeded, it trips to `OPEN` (fast-failing calls to protect downstream services). After a cooldown wait duration, it enters `HALF-OPEN` to test if downstream services have recovered.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Spring @RestController Annotation Composition",
        "slug": "spring-restcontroller-annotation-composition",
        "content": """In Spring MVC, `@RestController` is a convenience meta-annotation that combines `@Controller` and `@ResponseBody`.

Therefore, methods inside a `@RestController` automatically serialize their return values directly to the HTTP response body (e.g. as JSON) rather than resolving view template names.

Is this statement **True** or **False**?
""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Backend Engineering & Spring"],
        "tags": ["spring-boot", "rest", "annotations", "mvc"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "@RestController is annotated with @Controller and @ResponseBody, meaning every handler method returns serialized data by default."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "@RestController does not resolve HTML views like Thymeleaf unless explicitly configured."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. In Spring Framework, `@RestController = @Controller + @ResponseBody`. Return values are converted to HTTP responses via `HttpMessageConverter` (e.g., Jackson for JSON).",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Spring Boot Auto-Configuration Conditional Annotations",
        "slug": "spring-boot-autoconfiguration-conditional-annotations",
        "content": """How does Spring Boot determine which default beans and configurations to register automatically during application bootstrap?

Which conditional annotations drive this auto-configuration mechanism?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Backend Engineering & Spring"],
        "tags": ["spring-boot", "auto-configuration", "conditional", "annotations"],
        "options": [
            {
                "id": "opt_class",
                "text": "`@ConditionalOnClass`: Activates a configuration only when specified classes are present on the classpath (e.g. `MongoTemplate.class` or `RedisConnectionFactory.class`)",
                "explanation": "Allows starter JARs on the classpath to activate corresponding configuration classes automatically."
            },
            {
                "id": "opt_missing_bean",
                "text": "`@ConditionalOnMissingBean`: Registers a default fallback bean only if the user has not declared their own custom `@Bean` of that type",
                "explanation": "Allows developers to override any Spring Boot default by declaring their own bean."
            },
            {
                "id": "opt_property",
                "text": "`@ConditionalOnProperty`: Checks whether a specific configuration property in `application.yaml` matches an expected value or is enabled",
                "explanation": "Used for flags like stacked.execution.docker.enabled=true."
            },
            {
                "id": "opt_override",
                "text": "Spring Boot ignores user beans and always forces built-in default beans to take precedence",
                "explanation": "False. User-defined beans always override auto-configured beans."
            }
        ],
        "correctOptionIds": ["opt_class", "opt_missing_bean", "opt_property"],
        "explanation": "Spring Boot's auto-configuration engine evaluates conditional annotations (`@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`). `@ConditionalOnMissingBean` ensures that user-defined beans take precedence over framework defaults.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "REST API Pagination: Offset vs Keyset (Cursor)",
        "slug": "rest-api-pagination-offset-vs-keyset",
        "content": """In REST API architecture and database design:

What are the critical trade-offs between **Offset-Based Pagination** (`LIMIT 20 OFFSET 100000`) and **Keyset/Cursor-Based Pagination** (`WHERE id > last_seen_id ORDER BY id ASC LIMIT 20`)?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Backend Engineering & Spring", "SQL & Relational Databases"],
        "tags": ["rest", "api-design", "pagination", "keyset", "performance"],
        "options": [
            {
                "id": "opt_perf",
                "text": "**Performance at scale**: Offset pagination requires scanning and discarding all previous rows (`OFFSET 100000` scans 100,020 rows), degrading to $\\mathcal{O}(N)$ time; Keyset pagination uses the index directly in $\\mathcal{O}(1)$ constant time",
                "explanation": "Deep offsets cause severe database CPU and I/O latency."
            },
            {
                "id": "opt_drift",
                "text": "**Data drift (Duplicate/Skipped items)**: If rows are inserted or deleted while a user navigates between pages, offset pagination can cause items to appear twice or be skipped; Keyset pagination provides stable navigation",
                "explanation": "New insertions shift row offsets, causing items to bleed into the next page."
            },
            {
                "id": "opt_jump",
                "text": "**Arbitrary page jumping**: Offset pagination easily allows jumping directly to page 50; Keyset pagination cannot jump to an arbitrary page without sequential navigation",
                "explanation": "Keyset pagination requires knowing the cursor of the preceding page."
            },
            {
                "id": "opt_sort",
                "text": "Keyset pagination works without any indexed columns or ordering",
                "explanation": "Keyset pagination strictly requires an ordered, indexed column or combination of columns."
            }
        ],
        "correctOptionIds": ["opt_perf", "opt_drift", "opt_jump"],
        "explanation": "Offset pagination is easy to implement and supports arbitrary page jumping (`page=5`), but suffers from $\\mathcal{O}(N)$ performance at deep offsets and data drift. Keyset (cursor) pagination provides stable $\\mathcal{O}(1)$ index traversal and avoids duplicate items, but cannot jump directly to arbitrary pages.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    }
]
