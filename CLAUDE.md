# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

Online Java Sandbox is a Spring Boot app that lets users write Java in the browser, compile/run it inside disposable Docker containers, and solve structured Java problems with GitHub OAuth login. It's an early-stage personal portfolio project (see `readme.md` for the full roadmap and status).

## Commands

Build, test, and run all go through the Maven wrapper (`mvnw` / `mvnw.cmd`), not a globally installed `mvn`.

```bash
./mvnw clean verify          # full build: compiles, runs Checkstyle, runs tests (what CI runs)
./mvnw test                  # run tests only
./mvnw test -Dtest=ClassName#methodName   # run a single test method
./mvnw checkstyle:check      # lint only, matches CI exactly (Windows: mvnw.cmd checkstyle:check)
node --test 'src/test/js/**/*.test.mjs'   # JS unit tests (editor autocomplete), Node 22+, no npm needed
```

Checkstyle (Google style, `google_checks.xml`) runs in the Maven `validate` phase, so it also fires implicitly before `test`/`verify`. It fails the build on any warning-level violation. See `checkstyle-settings-for-devs.md` for IDE setup (IntelliJ auto-applies the shared scheme in `.idea/codeStyles/`); the two things auto-formatting cannot fix are missing Javadoc on public classes/methods and import grouping mistakes it hasn't been run on yet.

### Running the app locally

Two modes, both documented in `readme.md`:

- **Dev mode** (no OAuth needed; requires a local RabbitMQ container, a local PostgreSQL at `127.0.0.1:5432/online_java` (override with `DEV_DATASOURCE_URL`), and a minimal `.env` with `RABBITMQ_USER`/`RABBITMQ_PASSWORD`, see `readme.md`): run `OnlineJavaApplication` with Spring profile `dev` active, then open `http://localhost:8080/sandbox`. This activates `security.DevSecurityConfig` instead of `security.SecurityConfig`: it permits all requests and, via `DevUserAuthenticationFilter`, signs every request in as a fixed `local-dev` user (the filter refuses to start unless `server.address` is loopback). `application-dev.properties` also disables Thymeleaf caching.
- **Production-like local setup**: `cp env.example .env`, fill in GitHub OAuth + Postgres credentials, then `docker compose up -d --build`. This uses `application-prod.properties` and requires real GitHub OAuth credentials and a running Postgres.

Either way, before first run: `docker pull eclipse-temurin:21-jdk` — this is the image the sandbox runner containers use, and code execution will fail without it. `docker ps` must work without `sudo`/elevated permissions.

Java 21 is the Maven-declared target (`pom.xml`), matching CI, the runner image and the Pi.

## Architecture

### Request flow for code execution

All code-execution classes live in `com.example.onlinejava.sandbox`. `SandboxController` (`POST /sandbox/run` and `POST /problems/{slug}/run`, plain-text body) does not run code directly -- it hands the source to `SandboxExecutionGateway`, which sends it to RabbitMQ and blocks (`RabbitTemplate.convertSendAndReceive`, using RabbitMQ's direct reply-to) until a reply arrives. `SandboxExecutionListener` consumes the queue on a separate thread and calls `JavaRunnerService.run()`, which:

1. Writes the source to a temp dir under `sandbox.root` as `Main.java` (submissions must define a public class `Main`).
2. Shells out to `docker run` via `ProcessBuilder` with a heavily locked-down container: `--network none`, `--cap-drop ALL`, `--pids-limit 32`, `--cpus 2`, `--read-only` root filesystem with only a small `tmpfs` at `/work` writable, and (in prod) `--security-opt no-new-privileges`. The source is bind-mounted read-only and copied into `/work` before `javac`/`java` run.
3. Waits up to `EXECUTION_TIMEOUT_SECONDS` (100s), force-kills and returns a timeout message if exceeded.
4. Always removes the named container and deletes the temp dir in a `finally` block, regardless of outcome.

The listener returns the result, which Spring routes back through RabbitMQ to the waiting gateway call. `RabbitMqConfig` enables `userCorrelationId` on the auto-configured `RabbitTemplate` -- without it, the template silently overwrites the caller's correlation ID with its own for internal reply-tracking. A random ID per request is propagated through SLF4J's MDC across the gateway, listener, and `JavaRunnerService`, so one submission's full journey is grep-able in `docker compose logs -f app` (see `readme.md`'s "How execution works").

`sandbox.root` and `sandbox.no-new-privileges` are externalized via `@Value` and differ between dev/prod properties files (dev disables `no-new-privileges` and points at `${user.home}/.online-java/runs`; prod uses `/tmp/online-java-runs`). Both dev and prod connect to RabbitMQ (dev at `127.0.0.1:5672`, prod at the `rabbitmq` Compose service name) -- execution now depends on RabbitMQ regardless of profile.

### Security configuration split

Two mutually exclusive `SecurityFilterChain` beans in `com.example.onlinejava.security`, selected by Spring profile:

- `SecurityConfig` (`@Profile("!dev")`): OAuth2 login required for everything except `/`, `/index.html`, `/css/login.css`, `/oauth2/**`, `/login/**`; CSRF is disabled specifically for `/sandbox/run` (since it's called via `fetch`/JS, not a form).
- `DevSecurityConfig` (`@Profile("dev")`): permits everything, disables CSRF entirely, and signs requests in as the `local-dev` user. Local-only.

`user.CurrentUserModelAdvice` adds the signed-in user's login to every page model as `username` (used by the navbar).

### Problem pages

Every problem is a `ProblemDefinition` class (in `problem.datastructures` or `problem.algorithms`) tagged with a `problem.Topic` and registered in `problem.ProblemRegistry`. It's rendered by one route (`/problems/{topicSlug}/{slug}`), one template (`problem.html`) and one script (`problem.js`); old category URLs get a 301 via `Redirects.permanent`. The hidden-test harness is built server-side by `buildTestSource`. Every new problem needs reference solutions at `src/test/resources/solutions/{slug}.txt` and `{slug}.wrong.txt` (method bodies), or `HarnessVerificationTest` fails. Never add one-off problem pages.

`Topic` (with its `Section`: Data Structures / Algorithms) drives the navbar menus (`problem.NavigationModelAdvice`), the `/problems` overview and the `/problems/{topicSlug}` topic pages. Topic slugs are public URLs.

### Users, discussions and attachments

- **Schema:** owned by Flyway (`src/main/resources/db/migration/V*.sql`). Hibernate runs with `ddl-auto=validate`. Never edit a merged migration; add a new `V{n}__*.sql`.
- **`user` package:** `AppUser` entity, upserted on each OAuth login by `AppUserLoginService`, which is plugged into `oauth2Login().userInfoEndpoint()`. The principal is `AppUserPrincipal`, an `OAuth2User` that carries `getUserId()`. `ProviderProfile` is the only place that knows GitHub attribute names.
- **`discussion` package:** per-problem two-level threads.
  - `thread_id` points at the root post; `reply_to_id` points at the post being quoted.
  - Supports soft delete and fixed reactions (`Reaction` enum).
  - `DiscussionService` holds all authorization and business rules. `DiscussionApiController` (`/api/...`) returns DTO records only.
  - `MarkdownRenderer` is the XSS boundary: commonmark with HTML escaped, then an OWASP allowlist. Only `/attachments/{uuid}` images survive.
- **`attachment` package:** screenshots stored as `bytea`.
  - `ImageSanitizer` sniffs the format, limits dimensions and re-encodes every image.
  - Uploads are unclaimed until a post embeds them. `OrphanAttachmentCleanup` purges unused ones after 24h.
  - Gotcha: the claim is a bulk update that clears the persistence context, so build any response *before* calling it.
- **`ratelimit.UserRateLimiter`:** in-memory Bucket4j limits per user and action.
- **Security headers:** `security.SecurityHeaders` defines the shared CSP, used by both profiles. A new CDN script must be from cdnjs or jsDelivr, pinned, with an SRI hash. Otherwise vendor it under `static/vendor/`.
- **`/api/**` in prod:** returns 401 instead of redirecting to login. Every state-changing `fetch` must send `csrfHeaders()` from `static/js/csrf.js`; the tokens come from the `fragments/csrf` meta fragment.

### Tests

- Controller tests use `@WebMvcTest`: `@Import(SecurityConfig.class)` for production rules, or `DevSecurityConfig` with profile `dev`.
- Repository and service tests use `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import(TestcontainersConfiguration.class)`, which runs a real PostgreSQL through Testcontainers, so Docker must be running.

`fragments/navbar.html` is a shared Thymeleaf fragment included via `th:replace` across pages.

### Code editor (Monaco)

- **Client:** the sandbox and problem pages use Monaco. It's vendored under `static/vendor/monaco-editor-*/` and regenerated by `scripts/vendor-monaco.sh`, which prunes unused language workers.
  - `static/js/editor/java-editor.js` (`createJavaEditor`) is the only place Monaco is configured: rainbow brackets, guides, IntelliJ keys (Ctrl+Alt+L format, Ctrl+Alt+O imports, Ctrl+Enter run, Ctrl+D duplicate), and the status bar.
  - Autocomplete logic lives in `static/js/editor/java-completions.js`: pure functions, unit-tested with `node --test`.
  - Gotcha: `editor.main` installs its own `MonacoEnvironment`, whose worker URL doesn't resolve under the AMD loader. `java-editor.js` replaces `getWorker` after load. Don't move that earlier.
  - Debug logs: run `localStorage.setItem("editor.debug", "1")` in the browser console.
- **Server:** the `editor` package.
  - `POST /api/editor/format` goes through `CodeFormattingService`, which rate-limits, then runs `ImportOrganizer` (syntax-only optimize imports), then `JavaFormatter` (Eclipse JDT, IntelliJ-like style).
  - `JavaSyntax.parseOrThrow` runs first because JDT silently returns an empty edit for broken code. Syntax errors return 422 with the line.

### CI/CD

`.github/workflows/ci-cd.yml`: on PR/push to `master`, runs Checkstyle (annotates violations via a GitHub Check even though the job doesn't hard-fail until a later explicit step) then `./mvnw clean verify` against a real Postgres service container. On push to `master` only, a second job deploys by SSHing (over Tailscale) into a Raspberry Pi, `git reset --hard origin/master`, and `docker compose up -d --build`. There is no staging environment — pushes to `master` deploy straight to the live Pi.
