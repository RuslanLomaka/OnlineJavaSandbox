console.log("problem.js loaded");

const mainContainer = document.querySelector("main[data-problem-slug]");
const hintButton = document.getElementById("hintButton");
const hintText = document.getElementById("hintText");
const runTestsButton = document.getElementById("runTestsButton");
const solutionCode = document.getElementById("solutionCode");
const testConsole = document.getElementById("testConsole");

// Converts the textarea into the CodeMirror Java code editor.
const editor = CodeMirror.fromTextArea(solutionCode, {
    mode: "text/x-java",
    theme: "material-darker",
    lineNumbers: true,
    matchBrackets: true,
    autoCloseBrackets: true
});

// Shows or hides the problem hint.
if (hintButton && hintText) {
    hintButton.addEventListener("click", () => {
        const isHidden = hintText.style.display === "none";

        hintText.style.display = isHidden ? "block" : "none";
        hintButton.textContent = isHidden ? "Hide Hint" : "Show Hint";
    });
}

// Runs the current problem solution on the backend.
if (runTestsButton && testConsole && mainContainer) {
    runTestsButton.addEventListener("click", async () => {
        const slug = mainContainer.dataset.problemSlug;
        const code = editor.getValue();

        runTestsButton.disabled = true;
        testConsole.textContent = "Running tests...";

        try {
            const response = await fetch(`/problems/${slug}/run`, {
                method: "POST",
                headers: {
                    "Content-Type": "text/plain"
                },
                body: code
            });

            const result = await response.text();

            if (!response.ok) {
                testConsole.textContent =
                    `Error (${response.status}): ${result}`;
                return;
            }

            testConsole.textContent = result;

        } catch (error) {
            testConsole.textContent =
                `Request failed: ${error.message}`;
        } finally {
            runTestsButton.disabled = false;
        }
    });
}