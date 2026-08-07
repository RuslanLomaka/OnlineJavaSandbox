# Online Java Sandbox

> A browser-based Java playground and programming-practice platform built with Spring Boot, Docker, PostgreSQL, Thymeleaf, and GitHub OAuth.

**Status:** Early development  
**Last updated:** 7 August 2026  
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

- Bubble Sort
- Longest Substring Without Repeating Characters

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
Execution timeout
Automatic cleanup
```

This is still an experimental project and not yet fully hardened for unrestricted public code execution.

## Current architecture

The project started as a fast prototype.

Some problem pages currently contain too many responsibilities:

- description;
- starter code;
- tests;
- Java source generation;
- console formatting;
- page-specific JavaScript.

The next major refactoring goal is to separate problem content from rendering and execution.

A future problem may look like:

```text
problems/
└── collections/
    └── longest-unique-substring/
        ├── problem.yaml
        ├── statement.md
        ├── starter.java
        └── tests.java
```

Then one generic controller and one generic template can render all problems.

## Roadmap

### Near term

- [ ] refactor the problem system;
- [ ] create reusable problem definitions;
- [ ] separate hidden tests from HTML;
- [ ] create a generic problem page;
- [ ] improve error handling;
- [ ] add output-size limits;
- [ ] add execution queue and rate limiting;
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

- [ ] restore working memory limits;
- [ ] limit concurrent runner containers;
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
- how CI/CD should work;
- why the current architecture needs refactoring.

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

## Local setup

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

## Manual deployment

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
- no execution queue;
- no strict output limit;
- no working memory limit on the current host;
- problem logic is still mixed with HTML and JavaScript;
- deployment is manual;
- CI/CD is not yet reliable.

## Author

Created by [Ruslan Lomaka](https://github.com/RuslanLomaka).

## License

A license has not yet been selected.