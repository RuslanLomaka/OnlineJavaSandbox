// Generic problem page: hint toggle, the method-body editor, Run Tests, and
// the discussion post count next to the "Discussion" link.
import { createJavaEditor, editorLog } from "./editor/java-editor.js";

const mainContainer = document.querySelector("main[data-problem-slug]");
const slug = mainContainer.dataset.problemSlug;
const hintButton = document.getElementById("hintButton");
const hintText = document.getElementById("hintText");
const runTestsButton = document.getElementById("runTestsButton");
const formatButton = document.getElementById("formatButton");
const editorHost = document.getElementById("solutionEditor");
const testConsole = document.getElementById("testConsole");

// Shows or hides the problem hint.
if (hintButton && hintText) {
    hintButton.addEventListener("click", () => {
        const isHidden = hintText.style.display === "none";
        hintText.style.display = isHidden ? "block" : "none";
        hintButton.textContent = isHidden ? "Hide Hint" : "Show Hint";
    });
}

let editor;

// Runs the user's method body against the problem's hidden tests (server side).
async function runTests() {
    runTestsButton.disabled = true;
    testConsole.textContent = "Running tests...";
    try {
        const response = await fetch(`/problems/${encodeURIComponent(slug)}/run`, {
            method: "POST",
            headers: {
                "Content-Type": "text/plain",
                // Required in production: this endpoint is CSRF-protected.
                ...csrfHeaders()
            },
            body: editor.getValue()
        });
        const result = await response.text();
        testConsole.textContent = response.ok ? result : `Error (${response.status}): ${result}`;
    } catch (error) {
        testConsole.textContent = `Request failed: ${error.message}`;
    } finally {
        runTestsButton.disabled = false;
    }
}

try {
    editor = await createJavaEditor(editorHost, {
        value: editorHost.dataset.starterCode ?? "",
        kind: "methodBody",
        onRun: runTests,
        statusElement: document.getElementById("editorStatus")
    });
    runTestsButton.addEventListener("click", runTests);
    formatButton.addEventListener("click", () => editor.format());
} catch (error) {
    editorLog.error("editor failed to load", error);
    editorHost.textContent = "The code editor failed to load. Please reload the page.";
    runTestsButton.disabled = true;
}

// Shows the number of discussion posts next to the "Discussion" link.
const discussionLink = document.getElementById("discussionLink");
if (discussionLink) {
    fetch(`/api/problems/${encodeURIComponent(slug)}/summary`, {
        headers: { Accept: "application/json" }
    })
        .then((response) => (response.ok ? response.json() : null))
        .then((summary) => {
            if (summary) {
                discussionLink.textContent = `💬 Discussion (${summary.postCount})`;
            }
        })
        .catch(() => {
            // The count is optional; leave the plain link.
        });
}
