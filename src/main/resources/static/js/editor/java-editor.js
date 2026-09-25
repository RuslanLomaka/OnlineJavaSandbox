/*
 * The one place the Monaco editor is configured for Java.
 *
 *   const editor = await createJavaEditor(hostElement, {
 *       value, kind: "class" | "methodBody", onRun, statusElement
 *   });
 *
 * Features: rainbow brackets, indentation/bracket guides, folding, sticky
 * scroll, minimap, autocomplete (java-completions.js), and IntelliJ keys:
 *   Ctrl+Alt+L   format (in the browser: Prettier + prettier-plugin-java)
 *   Ctrl+Enter   run            Ctrl+Space  suggestions
 *   Ctrl+D       duplicate line Ctrl+/      toggle comment
 *
 * Debugging: run localStorage.setItem("editor.debug", "1") in the console and
 * reload to see [editor] debug logs. Errors are always logged and shown in
 * the status bar under the editor.
 *
 * Requires on the page: Monaco's loader.js (classic <script>).
 * Everything here runs in the browser; the editor never calls the server.
 */
import { suggestionsAt } from "./java-completions.js";
import { formatJava } from "./java-formatter.js";

const MONACO_BASE = "/vendor/monaco-editor-0.56.0/min/vs";
const WORKER_URL = `${MONACO_BASE}/assets/editor.worker.js`;


export const editorLog = (() => {
    let debug = false;
    try {
        debug = window.localStorage.getItem("editor.debug") === "1";
    } catch (ignored) {
        // Storage unavailable (private mode); keep debug off.
    }
    return {
        debug: (...args) => debug && console.debug("[editor]", ...args),
        error: (...args) => console.error("[editor]", ...args)
    };
})();

let monacoPromise = null;

/** Starts Monaco's editor worker (tokenizing, diffing, link detection, ...). */
function createWorker(workerId, label) {
    const worker = new Worker(WORKER_URL, { name: label });
    worker.addEventListener("error", (event) => editorLog.error(
        "worker error", label, event.message, event.filename, event.lineno));
    editorLog.debug("worker started", label);
    return worker;
}

/** Loads Monaco once per page through its AMD loader. */
function loadMonaco() {
    if (!monacoPromise) {
        monacoPromise = new Promise((resolve, reject) => {
            if (typeof window.require !== "function" || !window.require.config) {
                reject(new Error("Monaco loader.js is missing from the page"));
                return;
            }
            window.require.config({ paths: { vs: MONACO_BASE } });
            window.require(["vs/editor/editor.main"], () => {
                // Must run after editor.main: it installs its own MonacoEnvironment,
                // whose editor-worker URL doesn't resolve under the AMD loader (the
                // editor then silently runs worker code on the main thread). The
                // worker is created lazily, so replacing getWorker here is in time.
                window.MonacoEnvironment = { ...window.MonacoEnvironment, getWorker: createWorker };
                registerJavaSupport(window.monaco);
                editorLog.debug("Monaco loaded");
                resolve(window.monaco);
            }, reject);
        });
    }
    return monacoPromise;
}

/** Source kind of each model, for the shared formatting provider. */
const modelKinds = new WeakMap();

/** Registers theme, autocomplete and formatting once per page. */
function registerJavaSupport(monaco) {
    monaco.editor.defineTheme("online-java-dark", {
        base: "vs-dark",
        inherit: true,
        rules: [],
        colors: {
            "editor.background": "#151515",
            "editorGutter.background": "#151515",
            // Rainbow brackets: one colour per nesting level, cycling.
            "editorBracketHighlight.foreground1": "#ffd700",
            "editorBracketHighlight.foreground2": "#da70d6",
            "editorBracketHighlight.foreground3": "#179fff",
            "editorBracketHighlight.foreground4": "#4ec9b0",
            "editorBracketHighlight.foreground5": "#f78c6c",
            "editorBracketHighlight.foreground6": "#c3e88d",
            "editorIndentGuide.background1": "#2e2e2e",
            "editorIndentGuide.activeBackground1": "#5a5a5a"
        }
    });

    const kinds = monaco.languages.CompletionItemKind;
    const kindMap = {
        method: kinds.Method, field: kinds.Field, variable: kinds.Variable,
        keyword: kinds.Keyword, snippet: kinds.Snippet, class: kinds.Class
    };
    // Lower sorts first: things in scope, then templates, keywords, classes.
    const sortGroup = { variable: "0", method: "0", field: "0", snippet: "1", keyword: "2", class: "3" };

    monaco.languages.registerCompletionItemProvider("java", {
        triggerCharacters: ["."],
        provideCompletionItems(model, position) {
            const result = suggestionsAt(model.getValue(), model.getOffsetAt(position));
            const word = model.getWordUntilPosition(position);
            const range = {
                startLineNumber: position.lineNumber,
                endLineNumber: position.lineNumber,
                startColumn: word.startColumn,
                endColumn: word.endColumn
            };
            editorLog.debug("completions", result.context, result.prefix, result.items.length);
            return {
                suggestions: result.items.map((item) => ({
                    label: item.label,
                    kind: kindMap[item.kind] ?? kinds.Text,
                    insertText: item.insertText,
                    insertTextRules: item.isSnippet
                        ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
                        : undefined,
                    detail: item.detail,
                    documentation: item.documentation ? { value: item.documentation } : undefined,
                    sortText: (sortGroup[item.kind] ?? "9") + item.label,
                    range
                }))
            };
        }
    });

    // Makes Monaco's own "Format Document" (and our Ctrl+Alt+L) use the server.
    monaco.languages.registerDocumentFormattingEditProvider("java", {
        async provideDocumentFormattingEdits(model) {
            const kind = modelKinds.get(model) ?? "class";
            const formatted = await formatCode(model.getValue(), kind);
            return formatted === null ? [] : [{ range: model.getFullModelRange(), text: formatted }];
        }
    });
}

/**
 * Formats code in the browser. Returns the new code, or null on failure (the
 * reason, e.g. "Line 3, column 12: syntax error", is shown in the status bar).
 */
async function formatCode(code, kind) {
    const started = performance.now();
    const status = activeStatus;
    try {
        const formatted = await formatJava(code, kind);
        editorLog.debug("formatted", { kind, ms: Math.round(performance.now() - started) });
        status?.show(formatted === code ? "Already formatted" : "Code formatted", "ok");
        return formatted;
    } catch (error) {
        editorLog.error("format failed", error);
        status?.show(error.message, "error");
        return null;
    }
}

let activeStatus = null;

function createStatus(element, kind) {
    const hints = el("span", "editor-status-hints",
        "Ctrl+Alt+L format · Ctrl+Enter run · Ctrl+Space suggest");
    const message = el("span", "editor-status-message", "");
    message.setAttribute("role", "status");
    element.replaceChildren(hints, message);
    let timer = null;
    return {
        show(text, level) {
            message.textContent = text;
            message.dataset.level = level;
            clearTimeout(timer);
            if (level === "ok") {
                timer = setTimeout(() => { message.textContent = ""; }, 3000);
            }
        }
    };
}

function el(tag, className, text) {
    const element = document.createElement(tag);
    element.className = className;
    element.textContent = text;
    return element;
}

/**
 * Creates a Java editor inside `host`.
 *
 * @param {HTMLElement} host element the editor fills (give it a height via CSS)
 * @param {object} options
 * @param {string} options.value initial code
 * @param {"class"|"methodBody"} options.kind whole file, or a method body
 * @param {function} [options.onRun] called on Ctrl+Enter
 * @param {HTMLElement} [options.statusElement] where hints/messages are shown
 * @returns {Promise<object>} small API: getValue, setValue, focus, format,
 *   onDidChange, monacoEditor
 */
export async function createJavaEditor(host, { value, kind = "class", onRun, statusElement }) {
    const monaco = await loadMonaco();
    host.replaceChildren(); // remove the "Loading editor…" placeholder
    const status = statusElement ? createStatus(statusElement, kind) : null;

    const editor = monaco.editor.create(host, {
        value,
        language: "java",
        theme: "online-java-dark",
        automaticLayout: true,
        fontFamily: "'JetBrains Mono', Consolas, 'Courier New', monospace",
        fontSize: 14,
        lineHeight: 21,
        tabSize: 4,
        insertSpaces: true,
        detectIndentation: false,
        bracketPairColorization: { enabled: true, independentColorPoolPerBracketType: false },
        guides: {
            indentation: true,
            highlightActiveIndentation: true,
            bracketPairs: "active",
            bracketPairsHorizontal: "active"
        },
        matchBrackets: "always",
        autoClosingBrackets: "always",
        autoClosingQuotes: "always",
        autoIndent: "full",
        folding: true,
        showFoldingControls: "mouseover",
        stickyScroll: { enabled: true },
        minimap: { enabled: kind === "class", renderCharacters: false, maxColumn: 80 },
        scrollBeyondLastLine: false,
        smoothScrolling: true,
        cursorBlinking: "smooth",
        renderWhitespace: "selection",
        quickSuggestions: { other: true, comments: false, strings: false },
        wordBasedSuggestions: "off",
        suggest: { showWords: false, preview: true },
        snippetSuggestions: "top",
        tabCompletion: "on",
        fixedOverflowWidgets: true,
        ariaLabel: kind === "class" ? "Java code editor" : "Java method body editor"
    });

    const model = editor.getModel();
    modelKinds.set(model, kind);
    editor.onDidFocusEditorText(() => { activeStatus = status; });
    activeStatus = status;

    const { KeyMod, KeyCode } = monaco;

    const format = async () => {
        activeStatus = status;
        await editor.getAction("editor.action.formatDocument").run();
    };

    editor.addAction({
        id: "online-java.format",
        label: "Format Code",
        keybindings: [KeyMod.CtrlCmd | KeyMod.Alt | KeyCode.KeyL],
        contextMenuGroupId: "1_modification",
        run: format
    });
    if (onRun) {
        editor.addAction({
            id: "online-java.run",
            label: "Run",
            keybindings: [KeyMod.CtrlCmd | KeyCode.Enter],
            run: () => onRun()
        });
    }
    // IntelliJ's Ctrl+D duplicates the line (Monaco's default selects the next match).
    editor.addCommand(KeyMod.CtrlCmd | KeyCode.KeyD,
        () => editor.trigger("keyboard", "editor.action.copyLinesDownAction", null));

    editorLog.debug("editor created", { kind });

    return {
        getValue: () => editor.getValue(),
        setValue: (code) => editor.setValue(code),
        focus: () => editor.focus(),
        format,
        onDidChange: (listener) => editor.onDidChangeModelContent(listener),
        monacoEditor: editor
    };
}
