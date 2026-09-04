# Online Java Sandbox

> A browser-based Java playground and programming-practice platform built with Spring Boot, Docker, PostgreSQL, Thymeleaf, and GitHub OAuth.

**Status:** Early development  
**Last updated:** 4 September 2026  
**Live demo:** https://java.ruslanlomaka.org

## What it does

Online Java Sandbox lets users:

- sign in with GitHub;
- write Java code in the browser;
- compile and run it inside disposable Docker containers;
- solve structured Java problems;
- see automatic test results.

Current problem sections:

- Arrays
- Collections
- Algorithms

Current problems:

- Bubble Sort (Arrays)
- Two Sum (Arrays)
- Binary Search (Arrays)
- Longest Substring Without Repeating Characters (Collections)

## Tech stack

- Java
- Spring Boot
- Spring Security
- GitHub OAuth
- Thymeleaf
- PostgreSQL
- Docker
- Docker Compose
- CodeMirror
- Cloudflare Tunnel
- Raspberry Pi
- Checkstyle (Google Java Style)
- SonarQube Cloud

## How execution works

```text
Browser
  ↓
Spring Boot
  ↓
Temporary Main.java
  ↓
Disposable Docker container
  ↓
javac + java
  ↓
Output returned to browser
```

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

## Current architecture

The project started as a fast prototype, and one part of the planned refactor is done: Arrays problems now go through a generic, data-driven path instead of one hand-written page per problem.

- Each Arrays problem is a small Java class (e.g. `BubbleSortProblem`) implementing `ProblemDefinition`, registered in `ProblemRegistry`.
- One controller route (`/problems/{category}/{slug}`) and one Thymeleaf template (`problem.html`) render every Arrays problem.
- One shared script (`problem.js`) drives the editor and submission flow for all of them.
- The full test harness (imports, student code, hidden tests) is assembled **server-side** (`ProblemDefinition.buildTestSource`) and only the student's method body is ever sent to the browser — hidden tests are no longer visible via view-source or the Network tab for these problems.

This has **not** happened yet for the Collections/Algorithms categories: `Longest Substring Without Repeating Characters` is still its own hand-written page, CSS file, and JS file, and it still assembles the complete test source (including hidden tests) client-side and posts it to the generic `/sandbox/run` endpoint — so the original "hidden tests aren't actually hidden" problem still applies to that one problem specifically.

Migrating the remaining problems to the same pattern used for Arrays is the next concrete step, not a redesign — the generic controller, template, and script already exist and just need to be reused.

## Roadmap

### Near term

- [x] create reusable problem definitions;
- [x] separate hidden tests from HTML (done for Arrays problems);
- [x] create a generic problem page;
- [x] add output-size limits;
- [x] add execution queue (concurrency limit);
- [ ] migrate the Collections/Algorithms problems onto the generic problem page;
- [ ] improve error handling;
- [ ] improve mobile layout;
- [ ] add more Java problems.

### Database and users

- [ ] create user entities;
- [ ] store GitHub user data;
- [ ] save attempts;
- [ ] track completed problems;
- [ ] add user profiles;
- [ ] add progress statistics;
- [ ] add comments and discussions.

### Security

- [x] limit concurrent runner containers;
- [ ] restore working memory limits (the `--memory` flag is set, but not currently enforced on the host running the containers);
- [ ] separate the runner from the web application;
- [ ] reduce Docker socket exposure;
- [ ] add abuse prevention;
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

Development mode is available for contributors who want to run and change the project locally without configuring PostgreSQL, GitHub OAuth credentials, an `.env` file or a production deployment.

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

### Run with IntelliJ IDEA

1. Open the project in IntelliJ IDEA.
2. Select JDK 21 as the project SDK.
3. Open the `OnlineJavaApplication` run configuration.
4. Set **Active profiles** to `dev`.
5. Run `OnlineJavaApplication`.
6. Open `http://localhost:8080/sandbox`.

Development mode:

- bypasses GitHub login;
- does not connect to PostgreSQL;
- disables template and static-resource caching;
- binds the web server to `127.0.0.1`;
- runs submitted Java code in restricted Docker containers;
- disables networking inside runner containers.

Development mode is intended for trusted local machines. Stop the application when you are finished working.

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
```

Do not commit `.env`.

## Useful commands

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f app
docker compose logs -f postgres
docker compose down
```

## CI/CD

Every pull request against `master` runs Checkstyle, the full test suite against a real PostgreSQL service container, and a SonarQube Cloud analysis with a quality gate that blocks merging on new bugs, vulnerabilities, or security hotspots. Pushes to `master` additionally deploy automatically: GitHub Actions connects to the Raspberry Pi over Tailscale, resets it to `origin/master`, and runs `docker compose up -d --build`. There is no staging environment — a merge to `master` goes straight to the live site.

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
- no comments;
- no user profiles;
- no working memory limit on the current host (the container flag is set but not enforced there);
- problem logic is still mixed with HTML and JavaScript for the Collections/Algorithms problems (Arrays problems are already migrated to the generic architecture, see [Current architecture](#current-architecture));
- no database or user entities yet (still file/code-defined problems, nothing persisted).

## Author

Created by [Ruslan Lomaka](https://github.com/RuslanLomaka).

## License

A license has not yet been selected.
