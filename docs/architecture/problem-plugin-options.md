# Architecture options: plug-and-play problems (Java now, other languages later)

## Context

Every new problem currently means writing a new Thymeleaf page, a new CSS file, a new JS
file, and a new `@GetMapping` in `PageController`. Verified by reading both existing
problems (`arrays/bubble-sort.*`, `collections/longest-unique-substring.*`):

- `bubble-sort.js` and `longest-unique-substring.js` are ~90% identical boilerplate
  (CodeMirror setup, fetch to `/sandbox/run`, button wiring). The only real difference is a
  `createCompleteJavaSource()` template literal that hand-assembles the full `Main.java`,
  including the **hidden tests**, entirely in client-side JavaScript.
- That means "hidden tests" are not hidden at all — anyone can read them via view-source or
  the Network tab. This is a real correctness/integrity problem, not just duplication.
- `SandboxController` / `JavaRunnerService` are already problem-agnostic: they take a raw
  Java source string and run it in a locked-down container. Good foundation — they don't
  need to know what a "problem" is.
- No JPA `@Entity` exists anywhere in the codebase yet, despite `spring-boot-starter-data-jpa`
  being a dependency. Adding DB-backed problems would be the first real entity in this app.
- `readme.md`'s own roadmap already names this as the top priority and sketches a
  file-based shape (`problem.yaml` + `statement.md` + `starter.java` + `tests.java`) rendered
  by one generic controller/template — but doesn't address the DB, the single-JS-file goal,
  or multi-language plug-in support the user now wants layered on top.

This document lays out options for each of those four architectural questions, with
pros/cons, so a direction can be picked. It does not implement anything — implementation
would be a separate, incremental follow-up (small steps, each explained before it's made).

---

## Decision A — where problem definitions live (source of truth)

**A1. Filesystem only.** Each problem is a folder (`problems/arrays/bubble-sort/` with
`problem.yaml`, `statement.md`, `starter.java`, `tests.java` or similar), loaded into memory
at startup. No DB table for problems at all.
- Pros: matches how contributors would want to add problems (open a PR with new
  files) — reviewable diffs, full git history per problem, zero admin UI needed, nothing new
  to migrate/seed.
  Simplest possible mental model; no schema to design yet.
- Cons: doesn't get a DB row to join against for "attempts"/"completed problems" (already on
  the roadmap under "Database and users") — a `problem_slug` string FK would be needed
  instead of a real `problem_id`. Doesn't use the DB for problems at all.

**A2. Database only.** A `Problem` JPA entity is the source of truth; problems are
authored by inserting rows (via an admin UI, or manual SQL/migration scripts).
- Pros: real relational FK for future attempts/progress/comments; one storage mechanism to
  reason about; natural fit if problems should be editable at runtime without a redeploy.
- Cons: loses git-reviewable diffs for problem content (statement text, hidden tests) —
  contributions either need a hand-written SQL migration per problem or an admin UI that
  doesn't exist yet. Hidden tests sitting as plain rows in Postgres are just as "visible" to
  anyone with DB access — fine, it's a private DB — but test content now depends on
  migration tooling (Flyway/Liquibase, neither present in `pom.xml` yet) to move safely
  between dev/prod.

**A3. Hybrid — files are authored, DB is the runtime index (recommended).**
Problems are still authored as files in the repo (same shape as A1 — PR-reviewable,
matches the "contribute a problem" onboarding story in the readme). On startup (or via a
small admin command), the app reads those files and upserts them into a `Problem` table
keyed by slug. The DB row is what the rest of the app (attempts, progress, future comments)
has a real foreign key to; the files remain the thing a contributor edits and reviews.
- Pros: keeps the contribution workflow already advertised in the readme (PR a folder of
  files); gives the real `problem_id` FK the "Database and users" roadmap section needs; DB
  becomes additive infrastructure, not a blocker for adding problem #3.
- Cons: two moving parts instead of one (a sync step can drift if someone edits a DB row
  directly instead of a file — mitigate by treating the DB rows as fully disposable/
  regenerable, never hand-edited).

---

## Decision B — server-side rendering & execution (the "plug-and-play problem render class")

**B1. One generic controller + one generic template, execution stays as-is.**
Replace the per-problem `@GetMapping`s in `PageController` with a single
`@GetMapping("/problems/{category}/{slug}")` that looks up a `Problem` (from whichever
store Decision A picks) and renders one `problem.html` populated from it. The
`/sandbox/run` endpoint and its "JS builds the whole source" pattern stay unchanged.
- Pros: smallest change; directly fixes the "new page/controller per problem" duplication.
- Cons: does **not** fix the hidden-tests-leak-to-the-browser problem, because the harness
  assembly stays in JS.

**B2. Generic controller + server-side harness assembly (recommended).**
Same generic controller/template as B1, but the hidden tests and `Main.java` assembly move
server-side. A new `ProblemRunnerService` takes `(problemId, submittedMethodBody)`, looks up
the problem's stored imports/signature/hidden tests, builds the full `Main.java` string
itself (same string-building logic currently in the JS template literals, just moved to
Java), and calls the existing `JavaRunnerService.run(...)` unchanged. The frontend posts
only the student's method body to a new endpoint, e.g. `POST /problems/{slug}/run`, and gets
back the same plain-text console output it does today.
- Pros: fixes the real security/integrity gap (hidden tests never reach the browser); the
  "plug-and-play problem class" concept is exactly this `ProblemRunnerService` + a `Problem`
  model — add a problem by adding data, not code.
- Cons: slightly more work than B1 (need to port the harness-building logic from JS to Java
  for the two existing problems); response format (plain text console) stays coupled to
  Thymeleaf-rendered pages rather than a clean API.

**B3. Full JSON API, frontend becomes a pure client.**
`GET /api/problems/{slug}` returns problem JSON (statement, starter code, signature);
`POST /api/problems/{slug}/submit` runs tests server-side and returns *structured* JSON
(pass/fail per test) instead of a text blob for the frontend to regex/color. Thymeleaf would
only render an empty shell page; everything else happens client-side against the API.
- Pros: cleanest long-term contract; trivially supports a future non-Thymeleaf frontend or
  mobile client; structured results are easier to unit test than "does this string contain
  `[PASS]`".
- Cons: biggest rewrite of the three — also touches the console-formatting logic, which
  today free-rides on being plain text. Bigger jump than an incremental first move would
  usually favor; better framed as a *later* evolution of B2 once B2 is working, not a
  replacement for it now.

---

## Decision C — one shared frontend script

Requirement: adding a problem must not require writing a new `.js` file.

**C1. Single `problem.js`, config embedded in the page (recommended).**
The generic `problem.html` template (from Decision B) renders a small JSON blob into the
page — e.g. `<script type="application/json" id="problem-data">{"signature": "...",
"starterCode": "...", "imports": [...]}</script>` — containing only what's safe to expose
(never hidden tests, since those moved server-side in B2). One static `problem.js`, shared
by every problem page, reads that blob, wires up CodeMirror, and posts the method body to
`/problems/{slug}/run`.
- Pros: literally one script for all problems; no extra network round-trip; works whether
  problems come from files or DB (Decision A is invisible to the frontend).
- Cons: the template must remember to escape/serialize the JSON blob safely (Thymeleaf's
  `th:inline="javascript"` or a `<script type="application/json">` + `JSON.parse` handles
  this safely — just needs to be done once, correctly, in the shared template).

**C2. Single `problem.js`, config fetched via a small JSON endpoint.**
Same script, but instead of embedded JSON it does `fetch('/problems/{slug}/config')` on
load.
- Pros: slightly cleaner separation (page HTML has zero problem-specific data); the config
  endpoint is reusable if B3 is built later.
- Cons: one extra round-trip before the editor is usable; a second endpoint to maintain for
  something C1 gets for free from the page render.

**C3. Shared runtime library, thin per-problem config files (not recommended, listed for
completeness).** Keep a small JS file per problem, but it only exports a plain config
object; a shared `problem-runtime.js` does all the real work.
- Pros: smaller diff from today; no template-side JSON serialization needed.
- Cons: still requires a new file per problem — the exact thing to eliminate. Only worth
  considering as a fallback if C1/C2 hit an unforeseen blocker.

---

## Decision D — leaving room for other languages later

**D1. Build the `Language` abstraction now.** A `Language` strategy (Docker image, compile
command, run command, source filename, harness-wrapping convention) that
`JavaRunnerService` generalizes into a `ContainerRunnerService` parameterized by it; `Problem`
gets a `language` field from day one.
- Pros: the "plug in a new language" story is fully designed before it's needed.
- Cons: pure speculation right now — there is exactly one language and one Docker image in
  this codebase today. Designing an abstraction against a single implementation tends to
  guess the wrong seams.

**D2. Leave seams, don't build the abstraction yet (recommended).** Do the Decision A/B/C
work using names and fields that don't bake in "Java-only" assumptions — e.g. `Problem`
carries a `language` column even though `"java"` is the only value today; the new service is
named `ProblemRunnerService`, not `JavaProblemRunnerService`; `JavaRunnerService`/
`DockerExecutable` stay as they are rather than being prematurely split. When (if) a second
language actually shows up, extracting a `Language` interface out of the one real
implementation is a small, well-informed refactor instead of a guess.
- Pros: no wasted design effort on a hypothetical; still cheap to extract later since
  nothing hardcodes "java" past field names.
- Cons: if multi-language support turns out to be wanted very soon, there's a small amount
  of rework to introduce the abstraction retroactively (rename `JavaRunnerService`, extract
  the interface). Given it's a rename + extract-interface, this is low-cost compared to
  guessing the abstraction wrong today.

---

## How the recommended options fit together

A3 (files authored + synced to DB) + B2 (generic controller, server-side harness) + C1
(single JS, embedded config) + D2 (seams, no abstraction yet) gives one coherent shape:

- `Problem` (JPA entity, `language` field defaulted to `"java"`): slug, category, title,
  statement (markdown/HTML), method signature, imports, starter code, hidden tests, synced
  from files under something like `src/main/resources/problems/**`.
- `PageController`: one `@GetMapping("/problems/{category}/{slug}")` replaces the two
  (soon: many) hardcoded mappings, renders one `problem.html`.
- `ProblemRunnerService`: `run(slug, submittedMethodBody)` → looks up the `Problem`, builds
  the full `Main.java` (imports + student method + stored hidden-test harness), delegates to
  the unchanged `JavaRunnerService.run(source)`.
- New endpoint, e.g. `SandboxController` gains `POST /problems/{slug}/run` (or a new
  `ProblemController`) that calls `ProblemRunnerService` instead of taking raw source from
  the client.
- `problem.js` (one file, replacing `bubble-sort.js`/`longest-unique-substring.js`): reads
  starter code + signature from an embedded JSON blob, drives CodeMirror, posts only the
  method body to `/problems/{slug}/run`, formats the response (the existing
  `formatConsoleOutput` logic from `longest-unique-substring.js` is the more complete version
  and is the one worth generalizing).
- `/sandbox/run` and `JavaRunnerService` stay exactly as they are — the free-form sandbox
  page keeps working unchanged, since it never had a "problem" concept to begin with.

This is a recommendation to react to, not a locked-in decision — Decisions A/B/C/D above are
independent enough that a different option could be mixed in (e.g. A1 instead of A3 to skip
the DB for now and only add it once user accounts/progress tracking actually starts, which
is also a defensible, more incremental sequencing).

## Suggested incremental path (once a direction is picked, for a future session)

Small steps, each independently mergeable:
1. Introduce `Problem` as a plain Java record/class (no DB yet) + a generic
   `problem.html` + generic controller mapping, hand-populated with the two existing
   problems' data — proves out B1/C1 without touching execution yet.
2. Move harness assembly server-side (`ProblemRunnerService` + new run endpoint) for one
   problem at a time, deleting that problem's bespoke `.js` file once its logic is ported.
3. Only after both problems are migrated and working: decide whether to add the `Problem`
   JPA entity + file-sync (Decision A), since by then the generic model's real shape is
   proven rather than guessed.

## Verification

Each step above is independently testable by hand: load `/problems/arrays/bubble-sort`,
confirm the page renders identically, submit a correct/incorrect solution and confirm
`/problems/{slug}/run` returns the same console output the old `/sandbox/run`-based flow
did. No new automated tests are proposed here since this document is options-only.