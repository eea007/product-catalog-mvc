# Gap Report: product-catalog-reactive → product-catalog-mvc

- **Reference:** `C:\Projects\product-catalog-reactive` (WebFlux, R2DBC)
- **Target:** `C:\Projects\product-catalog-mvc` (Spring MVC, JPA)
- **Method:** Read-only analysis of every file under `src/`, plus `pom.xml`, `application.yaml`, `schema.sql`, `Dockerfile` and the GitHub workflow. No builds or tests were run.
- **Date:** 2026-09-25

Paths below are relative to each project root. `R:` means REACTIVE and `M:` means MVC.

---

## Summary

MVC is a scaffold. It has the six basic CRUD endpoints over an in-memory H2 database, and nothing else. REACTIVE adds these on top of CRUD:

- JWT security through Cognito
- Name search
- Currency-converted pricing through currency-service
- S3 image upload
- SNS "stock depleted" events
- An AI inventory assistant (Spring AI + Anthropic) that calls inventory-service
- Config and secrets from AWS Secrets Manager
- A PostgreSQL (RDS) database
- CloudWatch metrics
- A Dockerfile and a CI/CD pipeline
- Tests for the handler and service layers

REACTIVE also has a few defects and dead code (see §10). Fix or drop those deliberately when porting, rather than copying them as they are.

---

## 1. Build and runtime configuration

### 1.1 Versions and dependencies

| | REACTIVE | MVC |
|---|---|---|
| Spring Boot parent | 3.5.13 | 3.5.13 |
| Java | 17 | 17 |
| Artifact | `com.example:product-catalog-reactive:0.0.1-SNAPSHOT` | `com.example:product-catalog-mvc:0.0.1-SNAPSHOT` |
| BOMs imported | `spring-cloud-aws-dependencies` 3.3.0, `spring-ai-bom` 1.0.1 | none |
| Web stack | `spring-boot-starter-webflux` | `spring-boot-starter-web` |
| Persistence | `spring-boot-starter-data-r2dbc` + `r2dbc-postgresql` (runtime) | `spring-boot-starter-data-jpa` + `h2` (runtime) |
| Security | `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` | none |
| Observability | `spring-boot-starter-actuator`, `micrometer-registry-cloudwatch2` | `spring-boot-starter-actuator` |
| AWS | `software.amazon.awssdk:s3`, `sqs`, `sns` (all pinned to 2.25.0, outside the BOM); `io.awspring.cloud:spring-cloud-aws-starter`, `spring-cloud-aws-secrets-manager` | none |
| AI | `spring-ai-starter-model-anthropic` | none |
| Other | `lombok` (optional) | `lombok` (optional) |
| Test | `spring-boot-starter-test`, `reactor-test`, `spring-security-test` | `spring-boot-starter-test` |
| services-shared-lib | **not used** | **not used** |

Both projects use the same build plugins: `spring-boot-maven-plugin` with Lombok excluded, and `maven-compiler-plugin` with the Lombok annotation processor.

### 1.2 Server, profiles and deployment

| | REACTIVE | MVC |
|---|---|---|
| Server port | **8081** | **8080** |
| Shutdown | graceful | graceful |
| Spring profiles | **None.** No `application-*.yaml` and no `@Profile`. Two property flags act as switches instead: `schema.init.enabled` and `data.seed.enabled` (both default to true). | None |
| Resources | `application.yaml`, `schema.sql` | `application.yaml`, plus empty `static/` and `templates/` directories |
| Dockerfile | Two-stage `amazoncorretto:17-alpine` build using layertools extraction, a non-root `catalog` user, and `EXPOSE 8081` | none |
| CI/CD | `.github/workflows/deploy.yml`: `mvn test` on every push and PR. On push to `main` it builds the JAR, pushes to ECR `325879634980.dkr.ecr.us-east-2.amazonaws.com/product-catalog-reactive` using OIDC role `github-actions-product-catalog-deploy`, then deploys through SSM `AWS-RunShellScript` to EC2 instances tagged `Service=product-catalog` (runs `/opt/product-catalog/deploy.sh <sha>`) and polls for the result. | none |

### 1.3 application.yaml

**REACTIVE**

| Property | Value |
|---|---|
| `spring.application.name` | `product-catalog-reactive` |
| `spring.config.import` | `optional:aws-secretsmanager:product-catalog/rds,optional:aws-secretsmanager:product-catalog/anthropic` |
| `spring.r2dbc.url` | `r2dbc:postgresql://database-1.c5iou48aowj1.us-east-2.rds.amazonaws.com:5432/postgres?sslMode=require` |
| `spring.r2dbc.username` / `password` | `${username}` / `${password}` (from the RDS secret) |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `https://cognito-idp.us-east-2.amazonaws.com/us-east-2_1uhRbLPvP` |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `…/us-east-2_1uhRbLPvP/.well-known/jwks.json` |
| `spring.cloud.aws.region.static` | `us-east-2` |
| `spring.cloud.aws.secretsmanager.enabled` | `true` |
| `management.cloudwatch.metrics.export.namespace` / `step` | `ProductCatalog` / `5m` |
| `management.metrics.enable.all` | `false`, with `http.server.requests: true` and `jvm.memory: true` |
| `management.metrics.tags` | `application=${spring.application.name}`, `environment=production` |
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` |
| `management.endpoint.health.show-details` | `always` |
| `server.port` / `server.shutdown` | `8081` / `graceful` |
| `aws.region` | `us-east-2` |
| `aws.s3.bucket` | `product-catalog-images-dev-325879634980` |
| `aws.sns.stock-depleted-topic-arn` | `arn:aws:sns:us-east-2:325879634980:stock-depleted-topic` |
| `aws.sqs.reorder-events-url` | `https://sqs.us-east-2.amazonaws.com/325879634980/reorder-events-dev` (**never read by any code**) |
| `logging.level` | DEBUG for `org.springframework.r2dbc`, `org.springframework.ai`, `org.springframework.ai.chat.client`, `org.springframework.web.reactive.function.client`, `reactor.netty.http.client` |
| `currency.service.url` | `http://localhost:8083` |
| `inventory.service.url` | `http://localhost:8082` |

The code also needs these properties, which are **not in the YAML** and must come from Secrets Manager or the environment:

- `api-key`: the currency-service key. It has no default, so startup fails without it.
- `anthropic-api-key`: no default. Presumably it comes from the `product-catalog/anthropic` secret.
- `username` and `password`: from the `product-catalog/rds` secret.

The test class supplies placeholder values for `api-key` and `anthropic-api-key`.

**MVC**

| Property | Value |
|---|---|
| `spring.application.name` | `product-catalog-mvc` |
| `spring.datasource.*` | `jdbc:h2:mem:catalogdb`, `org.h2.Driver`, `sa` / `""` |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` |
| `spring.jpa.show-sql` / `hibernate.format_sql` | `true` / `true` |
| `spring.h2.console.enabled` | `true` |
| `management.endpoints.web.exposure.include` | `health,info,metrics` |
| `management.endpoint.health.show-details` | `always` |
| `server.port` / `server.shutdown` | `8080` / `graceful` |

---

## 2. REACTIVE endpoints (in router registration order)

`ProductRouter.routes` (`R: config/ProductRouter.java:15-25`):

| # | Method | Path | Handler | Auth | Behavior |
|---|---|---|---|---|---|
| 1 | GET | `/api/v1/products` | `ProductHandler::findAll` | JWT | 200 with a JSON array of products |
| 2 | GET | `/api/v1/products/category/{category}` | `ProductHandler::findByCategory` | JWT | 200 with an array (exact match on category) |
| 3 | GET | `/api/v1/products/search?name=` | `ProductHandler::searchByName` | JWT | 200 with an array. Case-insensitive "contains" match. A missing `name` becomes `""`, which matches every product. |
| 4 | GET | `/api/v1/products/{id}/price?currency=` | `ProductHandler::getProductWithCurrency` | JWT | 200 with a `ProductWithPrice`, 404 if the product doesn't exist. `currency` defaults to `USD`, which skips the remote call. |
| 5 | POST | `/api/v1/products/{id}/image` | `ProductHandler::uploadImage` | JWT | Raw bytes body; `Content-Type` defaults to `image/jpeg`. Returns 200 with the S3 URL as a string. |
| 6 | GET | `/api/v1/products/{id}` | `ProductHandler::findById` | JWT | 200, or 404 |
| 7 | POST | `/api/v1/products` | `ProductHandler::create` | JWT | 201 with the product. Returns 400 `{"error": "..."}` when the body contains a non-zero `id`. |
| 8 | PUT | `/api/v1/products/{id}` | `ProductHandler::update` | JWT | 200, or 404. Publishes an SNS event when the new stock is 0. |
| 9 | DELETE | `/api/v1/products/{id}` | `ProductHandler::delete` | JWT | 204, or 404 |

`AssistantRouter.assistantRoutes` (`R: assistant/AssistantRouter.java:14-16`) is a separate `RouterFunction` bean. Its path doesn't overlap the product routes.

| # | Method | Path | Handler | Auth | Behavior |
|---|---|---|---|---|---|
| 10 | POST | `/api/v1/assistant/chat` | `AssistantHandler::chat` | JWT | The body is a plain-text prompt. Returns 200 with the model's reply as a raw string, or 500 `Assistant unavailable: <msg>`. |

Actuator endpoints:

| Path | Auth |
|---|---|
| `/actuator/health`, `/actuator/info` | Public |
| `/actuator/metrics` | JWT required |
| `/actuator/prometheus` | Exposed in config, but there's no `micrometer-registry-prometheus` on the classpath, so the endpoint won't exist. |

Two notes on routing:

- Order matters in functional routing. `/search` and `/{id}/price` are registered before `/{id}`. In MVC, annotation specificity handles this automatically.
- Every route requires a valid Cognito JWT. There are **no role checks**: `cognito:groups` is mapped to `ROLE_*` authorities, but nothing uses them.

---

## 3. REACTIVE class inventory

### `com.example.catalog`

| Class | Responsibility | Calls |
|---|---|---|
| `ProductCatalogReactiveApplication` | `@SpringBootApplication` entry point | none |

### `com.example.catalog.config`

| Class | Responsibility | Calls |
|---|---|---|
| `AwsConfig` | Defines the `S3AsyncClient`, `SqsAsyncClient` and `SnsAsyncClient` beans for `${aws.region}` using `DefaultCredentialsProvider` | AWS SDK builders |
| `DataSeeder` | `CommandLineRunner` that seeds 3 products if the table is empty (blocks on `count()` and `saveAll`). Gated by `data.seed.enabled`. | `ProductRepository` |
| `ProductRouter` | Registers the 9 product routes | `ProductHandler` |
| `SchemaInitializer` | `ConnectionFactoryInitializer` that runs `schema.sql`. Gated by `schema.init.enabled`. | R2DBC `ConnectionFactory` |
| `SecurityConfig` | A highest-precedence `WebFilter` that answers every OPTIONS request with 200, plus the `SecurityWebFilterChain` (JWT resource server, Cognito group mapping, 401 entry point) | Spring Security, `JwtAuthenticationConverter` |
| `WebClientConfig` | Builds the `currencyServiceClient` `WebClient` (base URL, `X-API-Key` header, 5 s connect timeout, response-timeout filter) | Reactor Netty `HttpClient` |

### `com.example.catalog.domain`

| Class | Responsibility | Calls |
|---|---|---|
| `Product` | R2DBC entity for the `products` table: `id`, `name`, `category`, `price` (BigDecimal), `stockQuantity` | none |

### `com.example.catalog.model`

| Class | Responsibility | Calls |
|---|---|---|
| `ProductWithPrice` | Response DTO for `/price`: `id`, `name`, `category`, `originalPriceUsd`, `convertedPrice`, `currency`, `stockQuantity` | none |

### `com.example.catalog.repository`

| Class | Responsibility | Calls |
|---|---|---|
| `ProductRepository` | `ReactiveCrudRepository<Product, Long>` with `findByCategory`, `findByStockQuantityGreaterThan` (unused) and `findByNameContainingIgnoreCase` | none |

### `com.example.catalog.handler`

| Class | Responsibility | Calls |
|---|---|---|
| `ProductHandler` | Functional handlers for all product routes: builds responses and maps empty results to 404 and `IllegalArgumentException` to 400 | `ProductService`, `ProductImageService`, `CurrencyConversionService` |

### `com.example.catalog.service`

| Class | Responsibility | Calls |
|---|---|---|
| `ProductService` | CRUD and search. Create rejects a caller-supplied id. Update publishes a stock-depleted event when stock is 0. | `ProductRepository`, `StockEventPublisher` |
| `CurrencyConversionService` | Converts a USD amount through currency-service, with a 3 s timeout. Falls back to the original amount. | `currencyServiceClient` (WebClient) |
| `ProductImageService` | Uploads images to S3 (`uploadImage`) and deletes them (`deleteImage`, **which nothing calls**) | `S3AsyncClient` |
| `StockEventPublisher` | Publishes the `STOCK_DEPLETED` JSON to SNS | `SnsAsyncClient` |

### `com.example.catalog.assistant`

| Class | Responsibility | Calls |
|---|---|---|
| `AnthropicModelConfig` | Custom `AnthropicApi` and `AnthropicChatModel` beans | Spring AI Anthropic |
| `AssistantConfig` | Builds the `assistantChatClient` `ChatClient` with the system prompt and the default tools | `AnthropicChatModel`, `AssistantTools` |
| `AssistantHandler` | Handles `POST /assistant/chat`. Runs the blocking `ChatClient` call on `boundedElastic` and passes the caller's `Authorization` header through tool context. | `assistantChatClient` |
| `AssistantRouter` | Registers the chat route | `AssistantHandler` |
| `AssistantTools` | Two Spring AI `@Tool` methods that call inventory-service, forwarding the user's JWT | `WebClient` (from the auto-configured `WebClient.Builder`) |

### `com.example.catalog.assistant.model`

| Class | Responsibility | Calls |
|---|---|---|
| `StockLevel` | Mirrors inventory-service's `GET /api/v1/inventory/{id}` response: `id`, `productId`, `productName`, `reorderThreshold`, `reorderQuantity`, `status`, `lastUpdated` | none |
| `ReorderRecord` | Mirrors `GET /api/v1/inventory/{id}/reorder-estimate`: `productId`, `productName`, `reorderQuantity`, `unitPriceUsd`, `totalCostUsd`, `totalCostConverted`, `currency` | none |

The `schema.sql` resource defines the `products` table: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, `name VARCHAR(255)`, `category VARCHAR(255)`, `price DECIMAL(10,2)` and `stock_quantity INT`, all `NOT NULL`.

---

## 4. REACTIVE outbound integrations

### 4.1 HTTP calls to other services

| Target | Call site | URL property | Auth | Timeouts | Fallback |
|---|---|---|---|---|---|
| **currency-service** `GET /api/currencies/convert?from=USD&to={cur}&amount={amt}` | `CurrencyConversionService.convertPrice` | `currency.service.url` (default `http://localhost:8083`) | `X-API-Key: ${api-key}` static header | 5 s connect timeout (`ChannelOption.CONNECT_TIMEOUT_MILLIS`); 3 s total via `.timeout(Duration.ofSeconds(3))`; an 8 s "response timeout" filter that has no effect (see §10) | **On any error, returns the original USD amount.** The response still labels it with the requested currency. |
| **inventory-service** `GET /api/v1/inventory/{id}` | `AssistantTools.getStock` | `inventory.service.url` (default `http://localhost:8082`) | Forwards the caller's `Authorization` header from `ToolContext` | **None.** It uses the auto-configured `WebClient.Builder` and calls `.block()` with no timeout. | None. The exception propagates through Spring AI, so the chat endpoint returns 500. |
| **inventory-service** `GET /api/v1/inventory/{id}/reorder-estimate` | `AssistantTools.getReorderEstimate` | same | same | None | None |
| **Anthropic API** | `AnthropicChatModel` via `ChatClient.call()` | Spring AI default base URL | `anthropic-api-key` | Spring AI defaults | `AssistantHandler` catches every error and returns 500 `"Assistant unavailable: …"` |
| **Cognito JWKS** | Spring Security resource server | `jwk-set-uri` | none | defaults | Requests fail with 401 |

### 4.2 AWS SDK usage

| Service | Bean | Resource | Used by | Notes |
|---|---|---|---|---|
| **S3** | `S3AsyncClient` | Bucket `${aws.s3.bucket}` = `product-catalog-images-dev-325879634980`, key `products/{id}/image.jpg` | `ProductImageService.uploadImage` (and `deleteImage`, which is unused) | The key always ends in `.jpg`, whatever the content type. The URL is built as `https://{bucket}.s3.{region}.amazonaws.com/{key}`. It doesn't check that the product exists. Deleting a product doesn't delete its image. |
| **SNS** | `SnsAsyncClient` | Topic `${aws.sns.stock-depleted-topic-arn}` = `arn:aws:sns:us-east-2:325879634980:stock-depleted-topic` | `StockEventPublisher.publishStockDepleted`, called from `ProductService.update` | Subject `STOCK_DEPLETED`. Body `{"event","productId","productName","timestamp"}`, built with `String.formatted`, so `productName` isn't JSON-escaped. It fires on **every** update that leaves stock at 0, even if it was already 0. It doesn't fire on create. If publishing fails, the update returns an error even though the row is already saved. |
| **SQS** | `SqsAsyncClient` | `${aws.sqs.reorder-events-url}` = `…/reorder-events-dev` | **Nothing.** The bean and property exist, but no code sends or listens. | Dead configuration |
| **Secrets Manager** | spring-cloud-aws config import | `product-catalog/rds` (provides `username`, `password`) and `product-catalog/anthropic` (provides `anthropic-api-key`; `api-key` must come from one of these two) | `spring.config.import` | Both are `optional:`. If a secret is missing, startup fails later on the unresolved placeholders. |
| **CloudWatch** | `micrometer-registry-cloudwatch2` (auto-configured) | Namespace `ProductCatalog`, 5 m step | Actuator metrics | Exports only `http.server.requests` and `jvm.memory` |

All AWS clients use region `us-east-2` and the default credentials chain.

### 4.3 Concurrency

- **`Mono.zip`, `zipWith`, `Flux.merge` and parallel fan-out are not used anywhere.** Every chain is sequential.
- `AssistantHandler.chat` moves the blocking `ChatClient.call()` onto `Schedulers.boundedElastic()`.
- `AssistantTools` calls `.block()` on WebClient. That's safe only because the tools run on the `boundedElastic` thread.
- `DataSeeder` blocks during startup (`count().block()`, `saveAll(...).blockLast()`).
- The AWS async SDK futures are adapted with `Mono.fromFuture`.

The MVC port can therefore be written entirely as straightforward synchronous code. There's no fan-out to reproduce.

---

## 5. REACTIVE security, error handling and shared library

### 5.1 Security (`R: config/SecurityConfig.java`)

- **JWT:** OAuth2 resource server. Issuer is `https://cognito-idp.us-east-2.amazonaws.com/us-east-2_1uhRbLPvP`, and the JWKS URI is set explicitly.
- **Authorities:** the `cognito:groups` claim becomes `ROLE_<GROUP uppercased>`. No endpoint checks roles.
- **Public paths:** `OPTIONS /**`, `/actuator/health`, `/actuator/info`.
- **Protected paths:** everything else, via `anyExchange().authenticated()`.
- **Disabled:** CSRF, form login and HTTP Basic.
- **Unauthenticated requests:** the entry point returns a bare 401 (`HttpStatusServerEntryPoint`).
- **CORS:** Spring CORS is disabled (`CorsSpec::disable`). A `@Order(HIGHEST_PRECEDENCE)` `WebFilter` short-circuits every `OPTIONS` request with 200 but **adds no `Access-Control-Allow-*` headers**. The app therefore returns no CORS headers of its own, so something upstream (ALB, API Gateway or a proxy) presumably handles them. Confirm this before porting.

### 5.2 Error handling conventions

There's no global handler: no `@ControllerAdvice` and no `ErrorWebExceptionHandler`. Each handler maps errors itself.

| Situation | Response |
|---|---|
| Resource not found | `switchIfEmpty(ServerResponse.notFound())` returns 404 with an empty body |
| Create with a caller-supplied id | `IllegalArgumentException` becomes 400 with a hand-built `{"error": "<msg>"}` (not escaped) |
| Create / delete succeeds | 201 with the body / 204 |
| Non-numeric `{id}` | `Long.parseLong` throws `NumberFormatException`, and the default WebFlux error handler returns **500** |
| Currency-service failure | Swallowed. The USD amount is returned. |
| Image upload failure | Logged, then handled by the default error handler (500) |
| SNS publish failure | The update fails with a default 500 after the database write has happened |
| Assistant failure | 500 with a plain-text body labeled `application/json`. Successful replies are also raw strings labeled `application/json`. |
| Missing or invalid JWT | 401 with an empty body |

There's no bean validation (`@Valid` or constraints) anywhere.

### 5.3 services-shared-lib

**REACTIVE doesn't use it.** It's neither in the POM nor imported. The library (`com.portfolio:services-shared-lib:1.0.0`, published to GitHub Packages and used by `order-service`) provides:

- `CorrelationIdFilter` (servlet) and `CorrelationIdWebFilter` (reactive), with `CorrelationConstants`
- `SqsCorrelationHelper`
- `XRayConfig`

The servlet `CorrelationIdFilter` would suit MVC, but adopting it would go beyond parity.

---

## 6. REACTIVE inventory assistant (`com.example.catalog.assistant`)

**Model bean (`AnthropicModelConfig`)**

- `AnthropicApi` is built with `.apiKey(@Value("${anthropic-api-key}"))`. The key is a plain property with no default. In production it's expected to come from the `product-catalog/anthropic` Secrets Manager secret through `spring.config.import`. Tests set it to `test-placeholder-key`. The standard `spring.ai.anthropic.api-key` property is **not** used.
- A custom `AnthropicChatModel` bean uses model **`claude-sonnet-5`** (hard-coded), `maxTokens(2048)`, and thinking set to `DISABLED`. Because this bean is defined, the starter's auto-configured model steps aside. The starter is still on the classpath for its classes.

**Client (`AssistantConfig`)**

- `ChatClient` bean `assistantChatClient`. Its system prompt is: "inventory assistant … never guess a status, quantity, or cost — always call a tool … say plainly that restock dates aren't available."
- `defaultTools(assistantTools)`.

**Tools (`AssistantTools`, Spring AI `@Tool`)**

| Tool | Parameters | Calls | Returns |
|---|---|---|---|
| `getStock` | `Long productId`, `ToolContext` | `GET {inventory}/api/v1/inventory/{id}` | `StockLevel` |
| `getReorderEstimate` | `Long productId`, `ToolContext` | `GET {inventory}/api/v1/inventory/{id}/reorder-estimate` | `ReorderRecord` |

Both tools read `authorizationHeader` from the `ToolContext` and forward it, so inventory-service sees the end user's JWT.

**Request flow:** `POST /api/v1/assistant/chat` with a plain-text body → `AssistantHandler` reads the `Authorization` header → `chatClient.prompt(body).toolContext({authorizationHeader}).call().content()` on `boundedElastic` → 200 with the raw string.

Two quirks:

- `Map.of(...)` would throw an NPE if the header were missing. That can't happen today, because security rejects the request first.
- The assistant's WebClient has no timeout (see §4.1).

**MVC porting note:** `ChatClient.call()` is already blocking, so in MVC the handler becomes a plain `@PostMapping` with no scheduler hop. The tools can use `RestClient` instead of `WebClient` + `.block()`.

---

## 7. MVC class inventory

### `com.example.catalog`

| Class | Responsibility | Calls |
|---|---|---|
| `ProductCatalogMvcApplication` | `@SpringBootApplication` entry point | none |

### `com.example.catalog.config`

| Class | Responsibility | Calls |
|---|---|---|
| `DataSeeder` | `CommandLineRunner` that always seeds the same 3 products. There's no empty check and no `data.seed.enabled` flag; neither is needed with in-memory `create-drop`. | `ProductRepository` |

### `com.example.catalog.controller`

| Class | Responsibility | Calls |
|---|---|---|
| `ProductController` | `@RestController` at `/api/v1/products`: `GET /`, `GET /{id}`, `GET /category/{category}`, `POST /` (201), `PUT /{id}`, `DELETE /{id}` (204/404) | `ProductService` |

### `com.example.catalog.domain`

| Class | Responsibility | Calls |
|---|---|---|
| `Product` | JPA `@Entity` for `products`: `IDENTITY` id, non-null columns, `price` with precision 10 and scale 2 | none |

### `com.example.catalog.repository`

| Class | Responsibility | Calls |
|---|---|---|
| `ProductRepository` | `JpaRepository<Product, Long>` with `findByCategory` and `findByStockQuantityGreaterThan` (unused). **No `findByNameContainingIgnoreCase`.** | none |

### `com.example.catalog.service`

| Class | Responsibility | Calls |
|---|---|---|
| `ProductService` | `@Transactional` CRUD. **Doesn't** reject a caller-supplied id on create and **doesn't** publish events on update. | `ProductRepository` |

There are no `handler`, `model`, `assistant` or security classes, and no AWS or HTTP client configuration.

---

## 8. Gap table

Status is one of **Exists**, **Partial** or **Missing**. "MVC equivalent" suggests the idiomatic servlet-stack replacement.

### Build and configuration

| REACTIVE feature | MVC status | Notes / MVC equivalent |
|---|---|---|
| Spring Boot 3.5.13 / Java 17 | **Exists** | Same versions |
| Server port 8081 | **Partial** | MVC uses 8080. Align it if MVC is meant to replace REACTIVE behind the same target group. |
| Graceful shutdown | **Exists** | |
| PostgreSQL on RDS | **Missing** | MVC uses in-memory H2. Needs the `postgresql` JDBC driver and `spring.datasource.url=jdbc:postgresql://…?sslmode=require`. |
| `schema.sql` + `SchemaInitializer` (`schema.init.enabled`) | **Partial** | MVC relies on Hibernate `create-drop`. Use `spring.sql.init` or Flyway, with `ddl-auto: validate/none`. |
| `DataSeeder` idempotent and gated by `data.seed.enabled` | **Partial** | MVC seeds unconditionally, which would duplicate rows against a persistent database |
| Secrets Manager config import (`product-catalog/rds`, `product-catalog/anthropic`) | **Missing** | Add the spring-cloud-aws BOM, `spring-cloud-aws-starter` and `spring-cloud-aws-secrets-manager` |
| `spring.cloud.aws.region.static` / `aws.*` properties | **Missing** | |
| CloudWatch metrics export (namespace, step, filters, tags) | **Missing** | Add `micrometer-registry-cloudwatch2` and copy the `management.*` block |
| Actuator health/info/metrics | **Exists** | REACTIVE also lists `prometheus`, which doesn't actually work there (see §2) |
| H2 console | n/a | MVC only. Remove it or restrict it to a profile. |
| DEBUG logging for the HTTP client and AI | **Missing** | Use the servlet-stack logger names |
| Dockerfile | **Missing** | Copy it, and change the JAR name and `EXPOSE` |
| GitHub Actions test/ECR/SSM pipeline | **Missing** | Copy it, and change `ECR_REPO`, the service tag and the deploy script |

### Security

| REACTIVE feature | MVC status | Notes / MVC equivalent |
|---|---|---|
| OAuth2 JWT resource server (Cognito issuer and JWKS) | **Missing** | `spring-boot-starter-security` + `oauth2-resource-server`, `SecurityFilterChain` |
| `cognito:groups` → `ROLE_*` converter | **Missing** | `JwtAuthenticationConverter` (the servlet version, without the reactive adapter) |
| Public: OPTIONS, `/actuator/health`, `/actuator/info`; everything else authenticated | **Missing** | |
| CSRF, form login and Basic disabled; 401 entry point | **Missing** | `HttpStatusEntryPoint(UNAUTHORIZED)` |
| OPTIONS short-circuit filter, Spring CORS disabled | **Missing** | Confirm where CORS headers come from before copying this |

### API endpoints

| REACTIVE feature | MVC status | Notes / MVC equivalent |
|---|---|---|
| `GET /api/v1/products` | **Exists** | |
| `GET /api/v1/products/category/{category}` | **Exists** | |
| `GET /api/v1/products/search?name=` | **Missing** | Needs the repository method, the service method and the controller mapping |
| `GET /api/v1/products/{id}/price?currency=` | **Missing** | Needs `ProductWithPrice`, `CurrencyConversionService` and a currency client |
| `POST /api/v1/products/{id}/image` | **Missing** | Needs `ProductImageService`, an S3 client and `@RequestBody byte[]` |
| `GET /api/v1/products/{id}` | **Exists** | |
| `POST /api/v1/products` (201) | **Partial** | The 201 is there. The rejection of a caller-supplied id (400 `{"error":…}`) is missing. With JPA, a supplied id makes `save()` merge, which can overwrite an existing row. |
| `PUT /api/v1/products/{id}` | **Partial** | CRUD is there. The SNS stock-depleted publish is missing. |
| `DELETE /api/v1/products/{id}` | **Exists** | |
| `POST /api/v1/assistant/chat` | **Missing** | See the assistant rows below |

### Services and integrations

| REACTIVE feature | MVC status | Notes / MVC equivalent |
|---|---|---|
| `ProductService.findByName` | **Missing** | |
| `ProductService.create` id validation | **Missing** | |
| `ProductService.update` → `StockEventPublisher` when stock is 0 | **Missing** | |
| `CurrencyConversionService` (3 s timeout, fall back to USD) | **Missing** | `RestClient` with `JdkClientHttpRequestFactory` or `SimpleClientHttpRequestFactory` (connect 5 s, read 3 s), wrapped in try/catch |
| `WebClientConfig` currency client (base URL, `X-API-Key`, timeouts) | **Missing** | A `RestClient` bean. Note that it needs the `api-key` property. |
| `ProductImageService` (S3 upload; unused delete) | **Missing** | Synchronous `S3Client` |
| `StockEventPublisher` (SNS) | **Missing** | Synchronous `SnsClient`. Consider escaping the JSON properly with Jackson. |
| `AwsConfig` S3 / SNS / SQS clients | **Missing** | Synchronous clients. SQS isn't used by REACTIVE, so it can be skipped. |
| `ProductWithPrice` DTO | **Missing** | |
| `Product` entity fields | **Exists** | Same fields. MVC adds JPA column constraints. |
| `ProductRepository.findByNameContainingIgnoreCase` | **Missing** | |
| `ProductRepository.findByStockQuantityGreaterThan` | **Exists** | Unused in both |
| Error conventions (404 empty, 400 `{"error":…}`, 201, 204, 401) | **Partial** | 404, 201 and 204 exist. 400 and 401 are missing. |

### Inventory assistant

| REACTIVE feature | MVC status | Notes / MVC equivalent |
|---|---|---|
| Spring AI BOM 1.0.1 + `spring-ai-starter-model-anthropic` | **Missing** | |
| `AnthropicModelConfig` (`anthropic-api-key`, `claude-sonnet-5`, 2048 tokens, thinking disabled) | **Missing** | Can be copied as-is. It isn't stack-specific. |
| `AssistantConfig` `ChatClient` + system prompt + tools | **Missing** | Can be copied as-is |
| `AssistantTools` (`getStock`, `getReorderEstimate`, forwarding the JWT via `ToolContext`) | **Missing** | Replace `WebClient` + `.block()` with `RestClient`, and add a timeout |
| `StockLevel`, `ReorderRecord` | **Missing** | Can be copied as-is |
| `AssistantHandler` / `AssistantRouter` | **Missing** | Replace with one `@RestController` `@PostMapping("/api/v1/assistant/chat")`. No `boundedElastic` needed. |

### Other

| REACTIVE feature | MVC status | Notes / MVC equivalent |
|---|---|---|
| services-shared-lib | n/a | Not used by either project |
| Tests (see §9) | **Partial** | MVC has only `contextLoads` |

---

## 9. Test classes

### REACTIVE

| Test class | Type | What it covers |
|---|---|---|
| `ProductCatalogReactiveApplicationTests` | `@SpringBootTest` | `contextLoads`, with `api-key` and `anthropic-api-key` set to placeholders and `schema.init.enabled=false`, `data.seed.enabled=false` so it doesn't touch the database at startup |
| `handler/ProductHandlerTest` | `@WebFluxTest` + `@Import(ProductRouter, ProductHandler)` with mocked `ProductService`, `ProductImageService` and `CurrencyConversionService`. `WebTestClient.bindToRouterFunction(routes)`. | 9 tests: GET all (200 with 2 items); GET by id (200 / 404); GET by category (200 with 1 item); POST (201 and body); PUT (200 / 404); DELETE (204 / 404) |
| `service/ProductServiceTest` | Mockito unit test (`ProductRepository` and `StockEventPublisher` mocked, `StepVerifier`) | 10 tests: `findAll`; `findById` found / empty; `create` saves; `update` copies fields and saves; `update` returns empty when not found; `update` publishes `STOCK_DEPLETED` when stock is 0; `update` doesn't publish when stock > 0; `delete` true / false |

What these tests don't cover:

- `/search`, `/{id}/price`, `/{id}/image`, the 400 path on create, and non-numeric ids.
- `CurrencyConversionService` (including its fallback), `ProductImageService`, `StockEventPublisher`, `WebClientConfig`, `AwsConfig`.
- The whole assistant package.
- `SecurityConfig`. `bindToRouterFunction` bypasses the security `WebFilterChain`, so `@WithMockUser` on the handler test has no effect and no 401 or public-path behavior is tested.
- `ProductService.findByCategory`, `findByName`, and `create` rejecting an id.

### MVC

| Test class | Type | What it covers |
|---|---|---|
| `ProductCatalogMvcApplicationTests` | `@SpringBootTest` | `contextLoads` only |

**Tests needed for parity:**

- A `@WebMvcTest(ProductController)` equivalent of the 9 handler tests.
- A Mockito `ProductServiceTest` equivalent of the 10 service tests.
- For MVC, `@WebMvcTest` runs the security filter chain, so real 401 and public-path tests become straightforward. Add `spring-security-test` for them.

---

## 10. REACTIVE issues to decide on before copying

These were found in the reference implementation. Don't carry them over unintentionally.

1. **The response-timeout filter doesn't work** (`R: config/WebClientConfig.java:39-43`). `ofResponseProcessor(response -> Mono.just(response).timeout(8s))` applies the timeout after the response has already arrived, so it can never fire. The only effective bounds are the 5 s connect timeout and the 3 s `.timeout` in `CurrencyConversionService`.
2. **Currency fallback mislabels prices.** On failure, the USD amount is returned as `convertedPrice` with `currency` set to the requested code, for example `"EUR"`. The client can't tell the difference.
3. **The inventory WebClient has no timeout**, so a hung inventory-service blocks a `boundedElastic` thread and the chat request indefinitely.
4. **The SNS event is published outside any transaction**, and it's sent again on every update that leaves stock at 0. `productName` isn't JSON-escaped.
5. **The 400 body on create is built by string concatenation**, so it isn't escaped.
6. **A non-numeric `{id}` returns 500**, not 400.
7. **The SQS client and `aws.sqs.reorder-events-url` are dead config.** `ProductImageService.deleteImage` and `ProductRepository.findByStockQuantityGreaterThan` are dead code.
8. **The `prometheus` actuator endpoint is exposed but has no registry**, so it doesn't exist.
9. **The OPTIONS filter returns 200 without CORS headers.** Browsers will reject cross-origin calls unless an upstream component adds the headers.
10. **The assistant returns raw strings labeled `application/json`**, which isn't valid JSON, on both success and error.
11. **Roles are mapped from `cognito:groups` but never enforced.**
12. **The AWS SDK modules are pinned to 2.25.0 by hand** while the spring-cloud-aws 3.3.0 BOM brings its own SDK version. Consider letting the BOM manage `s3`, `sns` and `sqs` in MVC.
13. **The image key is always `image.jpg`**, whatever the upload's content type.
