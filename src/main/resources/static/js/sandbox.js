// Sandbox page: a whole-class Java editor plus Run / Format / Optimize imports.
// The code is kept in this browser's localStorage so a reload doesn't lose it.
import { createJavaEditor, editorLog } from "./editor/java-editor.js";

const STARTER_CODE = `import java.util.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello from the sandbox!");
    }
}
`;

const STORAGE_KEY = "sandbox.code";

const runButton = document.getElementById("runButton");
const formatButton = document.getElementById("formatButton");
const importsButton = document.getElementById("importsButton");
const resetButton = document.getElementById("resetButton");
const consoleOutput = document.getElementById("console");

function loadSavedCode() {
    try {
        return window.localStorage.getItem(STORAGE_KEY);
    } catch (ignored) {
        return null;
    }
}

function saveCode(code) {
    try {
        window.localStorage.setItem(STORAGE_KEY, code);
    } catch (ignored) {
        // Storage unavailable or full: the sandbox still works, it just won't remember.
    }
}

let editor;

async function runCode() {
    runButton.disabled = true;
    consoleOutput.textContent =
        "Compiling...\n" +
        "(If the sandbox is busy running other submissions, " +
        "this may take a few extra seconds — please wait.)\n";

    try {
        // /sandbox/run is CSRF-exempt, so no token is needed here.
        const response = await fetch("/sandbox/run", {
            method: "POST",
            headers: { "Content-Type": "text/plain;charset=UTF-8" },
            body: editor.getValue()
        });
        consoleOutput.textContent = await response.text();
    } catch (error) {
        consoleOutput.textContent = "Request failed:\n" + error.message;
    } finally {
        runButton.disabled = false;
    }
}

try {
    editor = await createJavaEditor(document.getElementById("editor"), {
        value: loadSavedCode() ?? STARTER_CODE,
        kind: "class",
        onRun: runCode,
        statusElement: document.getElementById("editorStatus")
    });

    let saveTimer = null;
    editor.onDidChange(() => {
        clearTimeout(saveTimer);
        saveTimer = setTimeout(() => saveCode(editor.getValue()), 500);
    });

    runButton.addEventListener("click", runCode);
    formatButton.addEventListener("click", () => editor.format());
    importsButton.addEventListener("click", () => editor.organizeImports());
    resetButton.addEventListener("click", () => {
        editor.setValue(STARTER_CODE);
        editor.focus();
    });
    editor.focus();
} catch (error) {
    editorLog.error("editor failed to load", error);
    document.getElementById("editor").textContent =
        "The code editor failed to load. Please reload the page.";
    runButton.disabled = true;
}
