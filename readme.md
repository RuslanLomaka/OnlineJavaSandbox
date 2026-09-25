# Online Java Sandbox

> A browser-based Java playground and programming-practice platform built with Spring Boot, Docker, PostgreSQL, Thymeleaf, and GitHub OAuth.

**Status:** Early development  
**Last updated:** 22 September 2026  
**Live demo:** https://java.ruslanlomaka.org

## What it does

Online Java Sandbox lets users:

- sign in with GitHub;
- write Java code in the browser;
- compile and run it inside disposable Docker containers;
- solve structured Java problems;
- see automatic test results;
- edit in a VS Code-style editor (Monaco): rainbow brackets, indentation guides,
  Java autocomplete, and Ctrl+Alt+L to reformat, all computed in the browser;
- discuss each problem with other users: threaded replies, Markdown with
  copyable code blocks, emoji, reactions and screenshots.

Problems are grouped into two navbar menus, **Data Structures** (Arrays,
Strings, Hash Maps & Sets, Stacks & Queues, Linked Lists, Trees, Graphs, Heaps)
and **Algorithms** (Sorting, Searching, Two Pointers, Sliding Window,
Recursion & Backtracking, Dynamic Programming, Greedy).

Current problems:

- Bubble Sort (Algorithms › Sorting)
- Binary Search (Algorithms › Searching)
- Longest Substring Without Repeating Characters (Algorithms › Sliding Window)
- Two Sum (Data Structures › Hash Maps & Sets)

## Tech stack

- Java
- Spring Boot
- Spring Security
- GitHub OAuth
- Thymeleaf
- PostgreSQL + Flyway migrations
- Docker
- Docker Compose
- RabbitMQ
- Monaco editor + Prettier (prettier-plugin-java), vendored, running in the browser
- Cloudflare Tunnel
- Raspberry Pi
- EasyMDE, highlight.js, DOMPurify, emoji-picker-element (discussion UI)
- commonmark-java + OWASP Java HTML Sanitizer (safe Markdown rendering)
- Bucket4j (rate limiting)
- JUnit 5, Spring MVC slice tests, Testcontainers (PostgreSQL)
- Checkstyle (Google Java Style)
- SonarQube Cloud
- CodeRabbit (AI pull-request review)

## How execution works

Code execution goes through RabbitMQ rather than being handled directly by the
web request thread:

```text
Browser
  ↓
Spring Boot controller (SandboxExecutionGateway)
  ↓
RabbitMQ queue
  ↓
Listener (SandboxExecutionListener) — same app process, separate thread
  ↓
Temporary Main.java
  ↓
Disposable Docker container
  ↓
javac + java
  ↓
Result sent back through RabbitMQ (direct reply-to)
  ↓
Output returned to browser
```

The listener runs in the same Spring Boot process as the web tier — this isn't
a separate deployable service (that's still a future roadmap item, see
[Security](#security)). RabbitMQ is used here as a real request/reply hop
(an RPC call over a message queue), not just for background events.

Every request is tagged with a random correlation ID, generated once per
submission and attached to every log line for that request's whole journey —
controller, queue, listener, container start/finish, and reply — via SLF4J's
MDC. Run `docker compose logs -f app | grep <the-id>` to see one submission's
full path end to end, which is especially useful for debugging on the
Raspberry Pi where there's no separate log aggregation tool.

Runner containers currently use:

```text
No network access
CPU restriction
Process restriction
Read-only filesystem
Dropped Linux capabilities
File descriptor and file size ulimits
Execution timeout
Concurrency limit (bounded number of containers running at once)
Output size cap (containers are killed if output exceeds the limit)
Automatic cleanup, plus a scheduled reaper as a safety net for orphaned
  containers/directories
Docker image pinned by digest, not by a mutable tag
```

Memory limits (`--memory`) are set on the container but are not
currently enforced on the host, so this is not yet a real resource
guarantee — see [Current limitations](#current-limitations).

This is still an experimental project and not yet fully hardened for unrestricted public code execution.

## Problem discussions

Every registered problem has a discussion page at
`/problems/{category}/{slug}/discussion`, linked from the problem page as
"Discussion (N)". Only users signed in with GitHub can read or post.

- **Users.** The first login creates an `app_user` row keyed by
  `(provider, provider_user_id)`, and each later login refreshes the
  name and avatar. `AppUserLoginService` hooks into Spring Security's
  OAuth2 login. The principal (`AppUserPrincipal`) carries the database id,
  which is used for ownership checks.
- **Threads.** Threads are two levels deep: a top-level post plus a flat list of replies.
  Replying to a reply keeps it in the same thread and shows a
  "↪ replying to @user" quote.
- **Content.** Posts are Markdown, with `java` code blocks (highlighted, with a
  Copy button), tables, links, emoji and pasted or dropped screenshots.
  Authors can edit or delete their own posts. Deletion is soft, so replies keep
  their context. There are six fixed reactions (👍 ❤️ 🎉 😄 🤔 🚀).
- **Storage.** Posts, reactions and screenshots live in PostgreSQL. The schema
  is managed by Flyway (`src/main/resources/db/migration`), and Hibernate only
  validates it.

Security measures:

- Markdown is rendered on the server with raw HTML escaped. The result then
  passes through an OWASP allowlist sanitizer, and the browser sanitizes it
  again with DOMPurify. Images may only point at this site's own
  `/attachments/{uuid}` URLs, so there are no tracking pixels.
- Screenshots are limited to 2 MB and 4096×4096 pixels. The format is detected
  from the bytes, not the file name. Every image is **re-encoded**, which strips
  EXIF/GPS metadata and anything hidden in the file. Images are served with
  `nosniff` and a sandboxing CSP. Uploads that no post uses are deleted
  after 24 hours.
- Edit and delete rights are checked on the server. Every state change needs a
  CSRF token. Posts, reactions and uploads are rate-limited per user
  (Bucket4j).
- Every page sends a strict Content-Security-Policy. Third-party scripts come
  from pinned CDN versions with SRI hashes.

## Current architecture

Every problem goes through one generic, data-driven path:

- Each problem is a small Java class (e.g. `BubbleSortProblem`) implementing `ProblemDefinition`, tagged with a `Topic`, and registered in `ProblemRegistry`.
- Problem URLs are `/problems/{topic}/{slug}` (e.g. `/problems/sorting/bubble-sort`), rendered by one template (`problem.html`) and one script (`problem.js`). Older category URLs redirect permanently.
- The full test harness (imports, student code, hidden tests) is assembled **server-side** (`ProblemDefinition.buildTestSource`); only the student's method body is ever sent to the browser, so hidden tests stay hidden.
- `HarnessVerificationTest` compiles and runs every harness with a known-correct and a known-wrong reference solution (`src/test/resources/solutions/`), so a broken harness fails the build.

Separately, code execution itself now goes through RabbitMQ as a real request/reply
hop instead of being called directly — see [How execution works](#how-execution-works)
for the full flow and why.

## Roadmap

### Near term

- [x] create reusable problem definitions;
- [x] separate hidden tests from HTML (done for Arrays problems);
- [x] create a generic problem page;
- [x] add output-size limits;
- [x] add execution queue (concurrency limit);
- [x] migrate every problem onto the generic problem page;
- [x] group problems into Data Structures / Algorithms topics;
- [ ] improve error handling;
- [ ] improve mobile layout;
- [ ] add more Java problems.

### Database and users

- [x] create user entities;
- [x] store GitHub user data;
- [ ] save attempts;
- [ ] track completed problems;
- [ ] add user profiles;
- [ ] add progress statistics;
- [x] add comments and discussions.

### Security

- [x] limit concurrent runner containers;
- [ ] restore working memory limits (the `--memory` flag is set, but not currently enforced on the host running the containers);
- [ ] separate the runner from the web application;
- [ ] reduce Docker socket exposure;
- [x] add abuse prevention for discussions (rate limits, sanitization);
- [ ] add stronger isolation.

### Collaboration

- [ ] contribution guide;
- [ ] pull-request template;
- [ ] issue templates;
- [ ] beginner-friendly tasks;
- [ ] contributor credits;
- [ ] public backlog;
- [ ] lightweight Scrum workflow when the team grows.

### Optional ideas

- [ ] AI-generated hints;
- [ ] compiler-error explanations;
- [ ] edge-case suggestions;
- [ ] code-review feedback;
- [ ] learning recommendations.

AI would be used only as a learning feature, not as a replacement for solving problems.

## Looking for contributors

I am looking for people who want to build a real portfolio project together.

This may be especially useful for developers who are looking for a job but do not yet have many projects they feel proud to show.

You do not need to be an expert.

I am ready to explain:

- how GitHub OAuth was configured;
- how Spring Security protects the application;
- how submitted Java code runs in Docker;
- how RabbitMQ is used as a request/reply hop for code execution, and how
  correlation-ID logging traces one request across it;
- how the Raspberry Pi deployment works;
- how PostgreSQL and Docker Compose are configured;
- how the CI/CD pipeline and quality gates work;
- why the remaining problem pages still need migrating to the generic architecture.

What I need from contributors:

- ideas;
- curiosity;
- time;
- willingness to discuss and improve the project.

Useful contribution areas include:

- Java problems;
- Spring Boot;
- frontend work;
- Docker;
- security;
- testing;
- documentation;
- architecture;
- UI/UX.

When three or more active contributors join, we can move to a lightweight Scrum-style process with a backlog, small tasks, reviews, and regular planning.

## Easy ways to help

You can start with something small:

- suggest a new problem;
- improve a task description;
- add edge cases;
- improve CSS;
- improve documentation;
- review architecture;
- help separate problem definitions from HTML;
- improve security;
- open an issue.

You do not need to configure the full production environment just to contribute an idea or problem.

## Development mode

Development mode is available for contributors who want to run and change the
project locally without configuring GitHub OAuth credentials or a production
deployment. It still needs a local RabbitMQ container and a small `.env` file
now, since code execution always goes through RabbitMQ regardless of profile
(see [How execution works](#how-execution-works)), plus a local PostgreSQL
for users and discussions.

### Requirements

- Git;
- JDK 17 or newer; JDK 21 is recommended;
- Docker Engine or Docker Desktop;
- IntelliJ IDEA or another Java IDE.

### Setup

Clone the repository:

```bash
git clone https://github.com/RuslanLomaka/OnlineJavaSandbox.git
cd OnlineJavaSandbox
```

Download the Java runner image before starting the application:

```bash
docker pull eclipse-temurin:21-jdk
```

Verify that Docker works without `sudo`:

```bash
docker ps
```

On Linux, if Docker reports a socket permission error, add your user to the Docker group:

```bash
sudo usermod -aG docker "$USER"
```

Log out and back in before continuing so the new group membership takes effect.

Create a `.env` file with just RabbitMQ credentials (dev mode doesn't need
the Postgres or GitHub OAuth values from `env.example`, only these two):

```env
RABBITMQ_USER=
RABBITMQ_PASSWORD=
```

Start RabbitMQ (only this one service, not the whole `compose.yaml` stack —
dev mode runs the app itself directly via IntelliJ, not in Docker):

```bash
docker compose up -d rabbitmq
```

The first run downloads the `rabbitmq` image automatically; no separate
`docker pull` needed. Confirm it's up with `docker compose ps`, and optionally
check the management UI at `http://localhost:15672` (log in with the
credentials from your `.env`).

Start a local PostgreSQL for users and discussions. Flyway creates the
tables on the first start:

```bash
docker run -d --name online-java-dev-db -p 127.0.0.1:5432:5432 \
  -e POSTGRES_DB=online_java -e POSTGRES_HOST_AUTH_METHOD=trust postgres:17
```

Dev mode connects to `jdbc:postgresql://127.0.0.1:5432/online_java` as user
`postgres` by default. To use a different database, set `DEV_DATASOURCE_URL`,
`POSTGRES_USER` and `POSTGRES_PASSWORD` in `.env`.

### Run with IntelliJ IDEA

1. Open the project in IntelliJ IDEA.
2. Select JDK 21 as the project SDK.
3. Open the `OnlineJavaApplication` run configuration.
4. Set **Active profiles** to `dev`.
5. Run `OnlineJavaApplication`.
6. Open `http://localhost:8080/sandbox`.

Development mode:

- bypasses GitHub login: every request is signed in as a fixed `local-dev`
  user, so posting and uploads work locally (the app refuses to start this
  way unless it is bound to `127.0.0.1`);
- connects to the local PostgreSQL started above;
- disables template and static-resource caching;
- binds the web server to `127.0.0.1`;
- connects to the RabbitMQ container started above (`127.0.0.1:5672`) to run
  submitted code, same request/reply flow as production uses;
- runs submitted Java code in restricted Docker containers;
- disables networking inside runner containers.

Development mode is intended for trusted local machines. Stop the application
(and, if you're done for a while, `docker compose stop rabbitmq`) when you are
finished working.

## Production-like local setup

```bash
git clone https://github.com/RuslanLomaka/OnlineJavaSandbox.git
cd OnlineJavaSandbox
cp env.example .env
docker compose up -d --build
```

Required `.env` values:

```env
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=

POSTGRES_DB=online_java
POSTGRES_USER=postgres
POSTGRES_PASSWORD=

SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/online_java
DOCKER_API_VERSION=1.41

RABBITMQ_USER=
RABBITMQ_PASSWORD=
```

Do not commit `.env`. Deploying to a new host (including the Raspberry Pi)
means setting all of these there manually — they are never carried over by a
`git pull`/deploy, since `.env` is gitignored on purpose.

## Useful commands

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f app
docker compose logs -f postgres
docker compose logs -f rabbitmq
docker compose down
```

To trace one code submission's full journey (web request through RabbitMQ to
the Docker container and back), grab the correlation ID from the start of any
log line and grep for it:

```bash
docker compose logs -f app | grep <correlation-id>
```

RabbitMQ's management UI is available at `http://<host>:15672` (Pi or
localhost) using the `RABBITMQ_USER`/`RABBITMQ_PASSWORD` from `.env` — useful
for watching queue depth while multiple submissions run concurrently.

## CI/CD

Every pull request against `master` runs Checkstyle, the full test suite against a real PostgreSQL service container, and a SonarQube Cloud analysis with a quality gate that blocks merging on new bugs, vulnerabilities, or security hotspots. Pushes to `master` additionally deploy automatically: GitHub Actions connects to the Raspberry Pi over Tailscale, resets it to `origin/master`, and runs `docker compose up -d --build`. There is no staging environment — a merge to `master` goes straight to the live site.

CI does not run a RabbitMQ service — verified that the test suite still passes without one reachable, since Spring AMQP retries connecting in the background rather than failing application startup. The only cost is a noisy (harmless) connection-refused stack trace in the CI test logs.

## Code review with CodeRabbit

Pull requests are also reviewed by [CodeRabbit](https://coderabbit.ai), an AI
reviewer that is free for public repositories. It posts a summary and line
comments on each PR, and you can talk to it by mentioning `@coderabbitai` in a
PR comment. `.coderabbit.yaml` sets it up and gives it security-focused
instructions for the security, discussion and attachment code. To enable it,
install the CodeRabbit GitHub App on the repository.

## Manual deployment

Only needed if the automatic deployment above isn't available:

```bash
cd ~/projects/OnlineJavaSandbox
git pull
docker compose up -d --build
```

## Current limitations

- no saved attempts;
- no scores;
- no user profiles;
- no working memory limit on the current host (the container flag is set but not enforced there);
- problems are still defined in code; only users and discussions are stored in the database;
- discussions don't update live; new messages appear after a page reload;
- code execution now has a new dependency: if RabbitMQ is down or unreachable, submissions fail gracefully (a clear error message, not a crash), but the app cannot run any code at all until it's back — a single point of failure that didn't exist when execution was a direct method call.

## Author

Created by [Ruslan Lomaka](https://github.com/RuslanLomaka).

## License

A license has not yet been selected.
