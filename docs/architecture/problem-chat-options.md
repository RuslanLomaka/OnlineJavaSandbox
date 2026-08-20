# Architecture options: per-problem discussion/chat

## Context

The goal is a discussion feature on problem pages: users can post messages, insert code
blocks, and reply to other messages — plus whatever else is genuinely useful. Constraints
from the request: there is no `User` entity yet, and while GitHub OAuth is the only login
method today, Google should be addable later without a redesign.

What's actually true in the codebase today, verified by reading the security config:

- `SecurityConfig` already requires OAuth2 login for everything except `/`, `/index.html`,
  `/css/login.css`, `/oauth2/**`, `/login/**` — so `/problems/**` is already
  login-gated in prod. A chat feature doesn't need new auth gating; it can rely on the
  existing `@AuthenticationPrincipal OAuth2User`.
- Only GitHub is registered (`spring.security.oauth2.client.registration.github.*` in
  `application-prod.properties`). `LoginController` and `PageController` both read
  `user.getAttribute("login")` — that attribute key is GitHub-specific. Google's OIDC
  attributes are different (`sub`, `name`, `email`, `picture`, no `login`). **Adding Google
  later will break both of those call sites**, not just whatever chat does — this needs a
  provider-agnostic "display name" lookup regardless of chat, so it's called out as a
  shared prerequisite below rather than a chat-only concern.
- No JPA entity exists yet anywhere (confirmed while researching the problem-plugin doc).
  Chat is the first feature that has no filesystem-authored alternative — messages are
  inherently user-generated, so they must live in a database from day one. This makes chat
  a forcing function for introducing the first entities, which also happens to be exactly
  what the readme's "Database and users" roadmap section already asks for.
- `spring.jpa.hibernate.ddl-auto=update` is in use, with no Flyway/Liquibase in `pom.xml`.
  Worth a note in the schema decision below, not a full redesign here.
- This is scoped to problem pages (`/problems/**`), not the free-form `/sandbox` page —
  one discussion thread per problem.

This document lays out options for five sub-decisions, with pros/cons, so a direction can
be picked before anything is built.

---

## Decision A — identity model (no `User` table exists yet)

**A1. Denormalized snapshot, no `User` entity.** Each `Message` row stores the OAuth
provider, provider user id, display name, and avatar URL directly, captured at post time.
No join table.
- Pros: nothing to build besides the `Message` table itself; fastest path to a working
  feature.
- Cons: a user's name/avatar in old messages never updates if they change it upstream;
  there's no natural place to hang moderation state (e.g. "banned") or attach other
  future per-user data (progress, profile) without a bigger migration later.

**A2. Introduce a minimal `User` entity now (recommended).** `User(id, provider,
providerUserId, displayName, avatarUrl, createdAt)`, upserted on every login (a small
`OAuth2LoginSuccessHandler` or a filter that runs once per login). `Message.authorId` is a
real FK to `User.id`. The natural key is the **pair** `(provider, providerUserId)`, not the
provider-specific display attribute — that pair is stable across GitHub and Google and
across a user renaming themselves.
- Pros: directly delivers the readme's own "create user entities; store GitHub user data"
  roadmap item, so this isn't scope creep beyond chat, it's pulling forward planned work
  that chat happens to need anyway; clean FK for messages; one obvious place to add Google
  later (`provider = "google"`, same table, no schema change) and one obvious place to add
  moderation flags or profile data later.
- Cons: slightly more upfront work than A1 (the upsert-on-login hook); it's a second entity
  to design correctly alongside `Message` in the same pass.

**A3. Defer the decision — nullable FK now, backfill later.** Build `Message` with a
nullable `authorId` plus the same denormalized snapshot columns as A1, so a `User` table can
be introduced later without migrating existing message data (new messages start pointing at
real users; old ones stay snapshot-only).
- Pros: avoids designing `User` under chat's time pressure.
- Cons: kicks a decision the app needs anyway (progress tracking already wants a user FK)
  further down the road for no real savings — the work in A2 is small enough that deferring
  it mostly just delays an already-planned entity.

**Shared prerequisite regardless of A1/A2/A3:** replace `user.getAttribute("login")` in
`LoginController`/`PageController` with a small provider-aware helper (e.g. `login` for
GitHub, fall back to `name`/`email` for OIDC providers like Google) — needed the moment a
second provider is registered, independent of chat.

---

## Decision B — message content & code blocks

**B1. Plain text + fenced code blocks only (recommended).** Users type `` ```java `` /
`` ``` `` fences same as GitHub/Slack; no other markdown. Server stores raw text as-is.
Rendering never uses `innerHTML` on user content — the renderer walks the text, and for
anything outside a fenced block it's inserted via `textContent`/`createTextNode`; only the
fenced code's language tag and body are pulled out and rendered into a `<pre><code>` element
(text still inserted as text, styled/highlighted via CodeMirror's `runMode`, which is
already loaded on problem pages).
- Pros: smallest possible XSS surface — there is no HTML-from-user-content path at all,
  ever; no new sanitizer dependency; still fully satisfies "insert code blocks".
- Cons: no bold/links/lists — a deliberate limitation, not an oversight.

**B2. Full Markdown.** A Markdown library (e.g. `commonmark-java` server-side, or `marked.js`
client-side) renders arbitrary Markdown to HTML, passed through an HTML allowlist sanitizer
(OWASP Java HTML Sanitizer, or DOMPurify client-side) before display.
- Pros: much richer formatting (bold, links, lists) alongside code blocks.
- Cons: introduces a real HTML-injection attack surface that has to be gotten right forever
  (sanitizer allowlist, library CVEs, keeping server-side and client-side sanitization
  consistent if both exist) — a heavier commitment than a discussion feature needs on day
  one, especially in a codebase that already treats code-execution security carefully.

**B3. Structured "insert code block" UI, no fence parsing at all.** The composer has an
explicit "insert code block" button (like Slack) that adds a distinct code-block segment to
the message; the message is stored as an ordered list of typed segments (`text` / `code`
with a language tag), not a single string with embedded fences.
- Pros: matches "insert code blocks" as a literal UI affordance rather than something users
  have to know Markdown-fence syntax to trigger; renderer has zero parsing to do (segments
  are already typed), which is both simpler and safer than B1's fence-scanning.
- Cons: more composer UI work than a plain `<textarea>` (needs a toolbar and a segment-aware
  editor, not just CodeMirror-for-the-whole-message); storing structured JSON per message is
  a slightly less "obvious" schema than a text column.

B1 and B3 are compatible, not mutually exclusive: B3's segment editor could still store to a
single fenced-text column at the DB layer (segments serialize to fences on save, parse back
to segments on load), getting B3's UI with B1's simple, sanitizer-free schema.

---

## Decision C — replies / threading

**C1. Flat list + "reply-to" quote reference (recommended).** All messages for a problem
are one flat, chronologically ordered list. `Message.parentMessageId` is a nullable
self-FK; if set, the UI renders a small "replying to @user: <snippet>" quote above the
message, same as GitHub PR review comments or Slack's inline reply. No recursion anywhere.
- Pros: one `ORDER BY created_at` query per problem, no recursive query or nested-set/
  materialized-path bookkeeping; trivial to paginate; directly satisfies "reply to
  messages".
- Cons: doesn't visually group a reply chain the way a nested thread view would.

**C2. Fully nested/recursive threads (Reddit-style).** Arbitrary-depth children.
- Pros: familiar "threaded discussion" feel for deep back-and-forth.
- Cons: needs a recursive CTE (or a nested-set/materialized-path column scheme) to fetch
  efficiently, and recursive rendering on the frontend; real complexity for a per-problem
  discussion that's unlikely to need deep nesting.

**C3. Two-level threads (top-level message + flat replies under it, no reply-to-a-reply).**
- Pros: a reasonable middle ground (Disqus/YouTube-lite style); slightly more structured
  than C1 without C2's recursion cost.
- Cons: still more query/render complexity than C1 for a benefit ("group replies under
  their topic") that C1's quote-reference already gives you visually, just less rigidly.

---

## Decision D — live updates

**D1. No live updates (recommended to start).** Messages load once per page view; posting
a message re-fetches/appends locally. Other users see new messages on their next page
load/manual refresh.
- Pros: zero new infrastructure; consistent with the project's current scale and the
  "small steps" approach used for the problem-plugin work.
- Cons: not real-time.

**D2. Short-interval polling.** `problem.js`-equivalent for chat does
`GET /problems/{slug}/messages?after={cursor}` every few seconds.
- Pros: near-real-time with no new server dependency, easy to layer on top of D1's endpoint
  later.
- Cons: periodic wasted requests; still not instant.

**D3. WebSocket (Spring `spring-boot-starter-websocket` + STOMP).** True push-based
real-time updates.
- Pros: real-time, and Spring's STOMP support integrates with the existing
  `@AuthenticationPrincipal` for per-connection identity.
- Cons: a new dependency and a new stateful-connection surface; the deployment path
  (Raspberry Pi behind a Cloudflare Tunnel, per the readme) would need the tunnel/reverse
  proxy confirmed to forward WebSocket upgrade headers correctly before this is viable —
  worth verifying as a spike, not assuming.

Recommendation: ship D1 first; D2 is a cheap near-real-time upgrade if the lack of live
updates actually bothers people in practice; D3 only if real-time turns out to matter enough
to justify the deployment-path work.

---

## Decision E — moderation & abuse controls

The security roadmap already lists "add abuse prevention" as a near-term item; a chat
feature is exactly where that becomes concretely necessary.

**E1. Minimal — login-gate only.** Posting requires the existing OAuth2 login; no rate
limit, no edit/delete.
- Pros: nothing extra to build.
- Cons: one determined logged-in user can flood a problem's thread; no recovery from a
  typo/mistake without an admin.

**E2. Self-service moderation + basic rate limit (recommended).** Authors can edit/soft-
delete their own messages within a time window; a simple per-user rate limit caps messages
per minute — the same shape as the `Semaphore`-based concurrency limit
`JavaRunnerService` already uses for sandbox executions, just counting messages instead of
running containers.
- Pros: covers the common cases (typos, spam bursts) without needing an admin role yet.
- Cons: soft-delete/edit-history adds a couple of columns and a small amount of UI.

**E3. Admin moderation.** A `role` column (or a hardcoded allowlist of GitHub logins) lets a
designated admin delete/hide any message.
- Pros: needed once there's real public traffic to moderate.
- Cons: not needed for an initial version with a small audience; adds an authorization
  dimension that's premature before E1/E2 are even in use.

---

## How the recommended options fit together

A2 (real `User` entity) + B1 (fenced code blocks, no HTML-from-user-content ever) + C1 (flat
list + reply-to quoting) + D1 (no live updates yet) + E2 (self-service edit/delete + rate
limit) gives one coherent, small first version:

- `User(id, provider, providerUserId, displayName, avatarUrl, createdAt)` — upserted on
  login; natural key is `(provider, providerUserId)`.
- `Message(id, problemRef, authorId FK->User, parentMessageId nullable self-FK, body text,
  createdAt, editedAt nullable, deletedAt nullable)` — `problemRef` is a slug (string) or a
  real `Problem.id` FK depending on which storage option the problem-plugin architecture
  doc ends up choosing; if that doc picks the DB-backed `Problem` option, this becomes a
  normal FK, otherwise it's a slug string, which still works fine for chat on its own.
- One shared `chat.js` (same "single script, config from the page" pattern as the
  problem-plugin doc's `problem.js`) handling posting, fence-aware rendering, and reply-to
  UI — reused across every problem page rather than written per problem, same principle as
  the earlier doc.
- `GET /problems/{slug}/messages` + `POST /problems/{slug}/messages` (and
  `PATCH`/`DELETE .../{messageId}` for E2's self-service editing) — a small new
  `ChatController`/`ChatService`, independent of `SandboxController`/`JavaRunnerService`.
- The GitHub-specific `user.getAttribute("login")` call sites get replaced with the
  provider-aware helper from Decision A regardless — this is worth doing as its own small
  step even before chat exists, since it's currently a latent bug waiting for Google to be
  added.

As with the problem-plugin doc, this is a recommendation to react to — each decision (A–E)
is independent enough to swap in a different option without invalidating the others.

## Suggested incremental path (once a direction is picked, for a future session)

1. Provider-agnostic display-name helper (fixes the existing GitHub-only assumption) —
   useful on its own, no chat dependency.
2. `User` entity + upsert-on-login — first real JPA entity in the app; small and testable
   in isolation (log in, confirm a row appears).
3. `Message` entity + `ChatController` (post/list only, no replies/edit/delete yet) — prove
   the read/write path and B1's safe rendering with the simplest possible feature slice.
4. Add reply-to (`parentMessageId` + quote UI), then edit/soft-delete + rate limiting (E2).
5. Revisit D2/D3 and E3 only if real usage shows they're needed.

## Verification

Each step is testable by hand: log in, confirm a `User` row is created; post a message with
a fenced code block, confirm it renders as code and not as interpreted HTML (try posting
`<script>alert(1)</script>` as a smoke test — it must render as literal text); reply to a
message and confirm the quote reference appears; confirm a second browser session (or
incognito) sees the same messages after a refresh.