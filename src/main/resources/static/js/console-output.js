/*
 * Terminal-style rendering of program / test output for the sandbox and
 * problem pages: colours each line by what it is (PASS/FAIL, compiler error,
 * stack trace, ...) and derives a status badge for the console header.
 *
 * Output is always inserted with textContent, never as HTML, so a program
 * printing "<script>" just shows that text.
 */

/**
 * Classifies one line of output.
 *
 * @param {string} line a single output line
 * @returns {string} one of: pass, fail, error, expected, actual, success,
 *   summary, exit-ok, exit-fail, warning, trace, meta, plain
 */
export function classifyLine(line) {
    const text = line.trim();
    if (text.startsWith("[PASS]")) return "pass";
    if (text.startsWith("[FAIL]")) return "fail";
    if (text.startsWith("[ERROR]")) return "error";
    if (text.startsWith("Expected:")) return "expected";
    if (text.startsWith("Actual:")) return "actual";
    if (text === "All tests passed!") return "success";
    if (/^Passed \d+ of \d+ tests?\.$/.test(text)) return "summary";

    const exit = /^Process finished with exit code (-?\d+)$/.exec(text);
    if (exit) return exit[1] === "0" ? "exit-ok" : "exit-fail";

    if (/\.java:\d+: warning:/.test(text) || text.startsWith("(output truncated")) return "warning";
    if (/\.java:\d+: error:/.test(text)
        || /^Exception in thread /.test(text)
        || /^([\w$]+\.)+[\w$]*(Exception|Error)(:|$)/.test(text)
        || /^Caused by: /.test(text)
        || /^\d+ errors?$/.test(text)
        || /^(Execution timed out|Execution was interrupted|Could not reach the execution queue|Docker process error|Request failed|Error \(\d+\))/.test(text)) {
        return "error";
    }
    if (/^at [\w$.<>]+\(/.test(text) || /^\.\.\. \d+ more$/.test(text) || /^\^+$/.test(text)) return "trace";
    if (/^(Compiling|Running tests|Waiting)\.\.\./.test(text) || /^\(If the sandbox is busy/.test(text)) return "meta";
    return "plain";
}

/**
 * Derives the console header badge from the whole output.
 *
 * @param {string} text full output
 * @returns {{state: "ok"|"fail"|"idle", label: string}}
 */
export function summarize(text) {
    const kinds = text.split("\n").map(classifyLine);
    const failed = kinds.filter((kind) => kind === "fail" || kind === "error").length;
    const hasTests = kinds.includes("pass") || kinds.includes("fail") || kinds.includes("summary");
    const exit = /Process finished with exit code (-?\d+)\s*$/.exec(text.trim());

    if (hasTests) {
        return kinds.includes("success") && failed === 0
            ? { state: "ok", label: "✔ Passed" }
            : { state: "fail", label: `✖ ${failed} failed` };
    }
    if (exit) {
        return { state: exit[1] === "0" ? "ok" : "fail", label: `exit ${exit[1]}` };
    }
    if (failed > 0) {
        return { state: "fail", label: "Error" };
    }
    return { state: "idle", label: "" };
}

/** Badge text shown in place of [PASS] / [FAIL] / [ERROR]. */
const BADGES = { pass: "PASS", fail: "FAIL", error: "ERROR" };

/**
 * Renders output into a console element and updates its status badge.
 *
 * @param {HTMLElement} body the <pre> console body
 * @param {string} text output to show
 * @param {{status?: HTMLElement, running?: boolean}} [options]
 */
export function renderConsole(body, text, { status, running = false } = {}) {
    const fragment = document.createDocumentFragment();
    for (const line of text.split("\n")) {
        const kind = classifyLine(line);
        const row = document.createElement("span");
        row.className = `console-line console-${kind}`;
        const badge = BADGES[kind];
        const match = badge && /^(\s*)\[(PASS|FAIL|ERROR)\]\s?(.*)$/.exec(line);
        if (match) {
            const tag = document.createElement("span");
            tag.className = `console-badge console-badge-${kind}`;
            tag.textContent = badge;
            row.append(match[1], tag, " " + match[3]);
        } else {
            row.textContent = line;
        }
        fragment.append(row, "\n");
    }
    body.replaceChildren(fragment);
    body.scrollTop = 0;

    if (status) {
        const summary = running ? { state: "running", label: "Running…" } : summarize(text);
        status.textContent = summary.label;
        status.dataset.state = summary.state;
    }
}
