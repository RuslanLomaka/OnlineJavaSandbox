# Checkstyle Setup for Developers

This project enforces Google's Java style rules (`google_checks.xml`) via
Checkstyle. Every pull request runs `mvn checkstyle:check` in CI
(`.github/workflows/ci-cd.yml`) and fails the build if there are violations.
This guide gets your editor auto-formatting to match those rules, so you
almost never have to fix a violation by hand.

## The fast path (do this first)

The project already ships a shared IntelliJ formatting profile in
`.idea/codeStyles/`. If you're using IntelliJ IDEA:

1. Pull the latest `master` (or your feature branch) and open the project.
2. IntelliJ auto-detects `.idea/codeStyles/Project.xml` and applies it —
   **no download, no import, no manual settings needed.**
3. To double check it's active: **Settings → Editor → Code Style → Java**.
   The scheme dropdown at the top should say **"Project"**, not "Default"
   or a personal scheme name.

That's it. From here on, before every commit:

- Press **Ctrl+Alt+O** (Optimize Imports) — fixes import order, removes
  unused imports.
- Press **Ctrl+Alt+L** (Reformat Code) — fixes indentation, wrapping,
  spacing.

Run those two shortcuts on any file you touched, and it will match
Checkstyle's rules automatically.

## Verifying before you push

Run this from the project root to check the whole codebase, exactly like
CI does:

```
./mvnw checkstyle:check
```

(On Windows without Git Bash, use `mvnw.cmd checkstyle:check` instead.)

`BUILD SUCCESS` and `You have 0 Checkstyle violations` means you're clear
to push.

## What auto-formatting CAN and CANNOT fix

Ctrl+Alt+O / Ctrl+Alt+L will fix, automatically, every time:

- Import order (all imports in one alphabetical block, statics separated)
- Indentation (2 spaces, 4 for wrapped lines)
- Operator wrapping (`+`, `&&`, etc. moved to the start of the next line)
- Javadoc continuation-line indentation

It will **not** fix:

- **Missing Javadoc comments.** Checkstyle requires a `/** ... */` comment
  above every public class and most public methods. No formatter can
  write documentation for you — if Checkstyle says
  `Missing a Javadoc comment`, you have to write a sentence or two by hand
  describing what the class/method does.

## Optional: live warnings in the editor

IntelliJ has a built-in **CheckStyle** tool window (icon on the left
sidebar). Set its "Rules" dropdown to **"Google Checks"** to see
violations highlighted as you type, without waiting for a Maven run.

**Caveat:** this panel uses its own bundled copy of the Google ruleset,
which can be a slightly different version than the one Maven actually
runs in CI. They mostly agree, but if the panel and
`./mvnw checkstyle:check` ever disagree, **trust the Maven command** —
that's what CI enforces and what actually blocks or passes a PR.

## Troubleshooting — problems we actually hit setting this up

**"Wrong lexicographical order for '...' import"**
Cause: imports grouped by source (e.g. all Spring imports first, then all
`java.*` imports) instead of one flat alphabetical list.
Fix: Ctrl+Alt+O, once the shared scheme is active.

**"'+' should be on a new line" (OperatorWrap)**
Cause: IntelliJ's default puts wrapped operators at the end of the
previous line; Google style wants them at the start of the next line.
Fix: this is included in the shared scheme now
(`Wrapping and Braces → Binary expressions → Operation sign on next
line`). If you ever rebuild the scheme from scratch, remember to check
that box.

**"Line continuation have incorrect indentation level" (Javadoc)**
Cause: IntelliJ's default aligns wrapped `@param`/`@throws` text under
the tag name; Checkstyle wants a flat 4-space indent instead.
Fix: also included in the shared scheme
(`JavaDoc tab → uncheck "Align parameter descriptions" and "Align thrown
exception descriptions" → check "Indent continuation lines"`).

**"Missing a Javadoc comment"**
Cause: a public class or method has no `/** ... */` comment above it.
Fix: write one. Keep it short — a one-sentence summary of what the
class/method does is enough for Checkstyle. For methods with parameters
or a return value, add `@param` / `@return` lines.

**IDE's CheckStyle panel flags something Maven doesn't (or vice versa)**
Cause: the IDE plugin's bundled Google ruleset and the version Maven
downloads can differ slightly between releases.
Fix: `./mvnw checkstyle:check` is the source of truth — if it passes,
you're fine, regardless of what the IDE panel says.

**GitHub Action "Report Checkstyle violations" fails with
`Resource not accessible by integration`**
Cause: the CI workflow's `GITHUB_TOKEN` only had `contents: read`
permission, but that step needs to write Checkstyle annotations back to
GitHub via the Checks API.
Fix: already applied — `checks: write` was added to the `permissions:`
block in `.github/workflows/ci-cd.yml`. Nothing you need to do here,
just noted in case it resurfaces on a future workflow change.

## Not using IntelliJ?

The shared scheme in `.idea/codeStyles/` only applies to IntelliJ. If
you're on a different editor, the source of truth is still
`google_checks.xml` (bundled inside the Checkstyle library Maven already
downloads — nothing to fetch yourself). Format however your editor
supports, then run `./mvnw checkstyle:check` before pushing to confirm.
