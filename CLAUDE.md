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
```

Checkstyle (Google style, `google_checks.xml`) runs in the Maven `validate` phase, so it also fires implicitly before `test`/`verify`. It fails the build on any warning-level violation. See `checkstyle-settings-for-devs.md` for IDE setup (IntelliJ auto-applies the shared scheme in `.idea/codeStyles/`); the two things auto-formatting cannot fix are missing Javadoc on public classes/methods and import grouping mistakes it hasn't been run on yet.

### Running the app locally

Two modes, both documented in `readme.md`:

- **Dev mode** (no Postgres/OAuth needed, but a local RabbitMQ container and a minimal `.env` with `RABBITMQ_USER`/`RABBITMQ_PASSWORD` are required, see `readme.md`): run `OnlineJavaApplication` with Spring profile `dev` active, then open `http://localhost:8080/sandbox`. This activates `security.DevSecurityConfig` (permits all requests, no login) instead of `security.SecurityConfig`, and `application-dev.properties` disables the datasource/JPA autoconfiguration and Thymeleaf caching.
- **Production-like local setup**: `cp env.example .env`, fill in GitHub OAuth + Postgres credentials, then `docker compose up -d --build`. This uses `application-prod.properties` and requires real GitHub OAuth credentials and a running Postgres.

Either way, before first run: `docker pull eclipse-temurin:21-jdk` — this is the image the sandbox runner containers use, and code execution will fail without it. `docker ps` must work without `sudo`/elevated permissions.

Java 17 is the Maven-declared target (`pom.xml`), but JDK 21 is what CI and the runner image use and is recommended for local dev.

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
- `DevSecurityConfig` (`@Profile("dev")`): permits everything, disables CSRF entirely. Local-only.

`PageController` reads the authenticated `OAuth2User` principal (nullable — null in dev mode, where a "Local developer" fallback name is used) to render pages.

### Problem pages (known tech debt — read before extending)

Each problem currently gets its own Thymeleaf template, CSS file, and JS file (e.g. `templates/arrays/bubble-sort.html` + `static/css/bubble-sort.css` + `static/js/bubble-sort.js`), wired to its own `@GetMapping` in `PageController`. A single template mixes problem description, starter code, hidden tests, Java source generation, and console formatting together. This is called out as the top refactoring priority in `readme.md`: the intended direction is data-driven problem definitions (e.g. `problem.yaml` + `statement.md` + `starter.java` + `tests.java` per problem) rendered by one generic controller/template, replacing the current one-page-per-problem duplication. Don't add more one-off problem pages/controllers/JS files without checking whether the user wants the generic version built first — ask rather than assuming.

Problem list pages (`/problems`, `/problems/arrays`, `/problems/collections`, `/problems/algorithms`) are static category listings that link to individual problem pages; `fragments/navbar.html` is a shared Thymeleaf fragment included via `th:replace` across pages.

### CI/CD

`.github/workflows/ci-cd.yml`: on PR/push to `master`, runs Checkstyle (annotates violations via a GitHub Check even though the job doesn't hard-fail until a later explicit step) then `./mvnw clean verify` against a real Postgres service container. On push to `master` only, a second job deploys by SSHing (over Tailscale) into a Raspberry Pi, `git reset --hard origin/master`, and `docker compose up -d --build`. There is no staging environment — pushes to `master` deploy straight to the live Pi.
