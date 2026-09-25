/*
 * In-browser Java formatter: Prettier + prettier-plugin-java, vendored under
 * /vendor/prettier-java-* (see scripts/vendor-prettier-java.sh). Code never
 * leaves the browser to be formatted.
 *
 * The libraries (~480 KB) are loaded on first use only.
 */

const DEFAULT_VENDOR_BASE = "/vendor/prettier-java-3.9.9-2.8.1/";

const OPTIONS = {
    parser: "java",
    tabWidth: 4,
    printWidth: 120,
    endOfLine: "lf"
};

// Method bodies aren't a complete Java file, so they are formatted inside
// this wrapper and cut back out. WRAPPER_PREFIX_LINES corrects error lines.
const WRAPPER_START = "class OnlineJavaSnippet {\nvoid onlineJavaSnippet() {\n";
const WRAPPER_END = "\n}\n}\n";
const WRAPPER_PREFIX_LINES = 2;
const BODY_INDENT = "        ";

/** Thrown for code that doesn't parse; message starts with "Line N". */
export class JavaFormatError extends Error {
}

const loaded = new Map();

/** Loads Prettier and the Java plugin once per base URL. */
function loadPrettier(vendorBase) {
    if (!loaded.has(vendorBase)) {
        loaded.set(vendorBase, Promise.all([
            import(`${vendorBase}prettier-standalone.mjs`),
            import(`${vendorBase}prettier-plugin-java.mjs`)
        ]).then(([prettier, plugin]) => ({ prettier, plugin: plugin.default ?? plugin })));
    }
    return loaded.get(vendorBase);
}

/**
 * Formats Java source.
 *
 * @param {string} code source to format
 * @param {"class"|"methodBody"} kind whole file, or only a method's statements
 * @param {{vendorBase?: string}} [options] where the vendored files live
 * @returns {Promise<string>} formatted code ending in a newline ("" for blank input)
 * @throws {JavaFormatError} if the code has a syntax error
 */
export async function formatJava(code, kind, { vendorBase = DEFAULT_VENDOR_BASE } = {}) {
    if (code.trim() === "") {
        return "";
    }
    const { prettier, plugin } = await loadPrettier(vendorBase);
    const isBody = kind === "methodBody";
    const source = isBody ? WRAPPER_START + code + WRAPPER_END : code;

    let formatted;
    try {
        formatted = await prettier.format(source, { ...OPTIONS, plugins: [plugin] });
    } catch (error) {
        throw toFormatError(error, isBody ? WRAPPER_PREFIX_LINES : 0);
    }
    return isBody ? unwrapBody(formatted) : formatted;
}

/** Cuts the method body back out of the formatted wrapper and removes its indent. */
function unwrapBody(formatted) {
    const lines = formatted.split("\n");
    const start = lines.findIndex((line) => line.includes("onlineJavaSnippet()")) + 1;
    const end = lines.lastIndexOf("    }");
    return lines.slice(start, end)
        .map((line) => (line.startsWith(BODY_INDENT) ? line.slice(BODY_INDENT.length) : line.trimStart()))
        .join("\n")
        .replace(/^\n+/, "")
        .replace(/\n*$/, "\n");
}

/** Turns the parser's message into "Line N, column M: syntax error". */
function toFormatError(error, lineOffset) {
    const match = /line:?\s*(\d+),?\s*column:?\s*(\d+)/i.exec(error.message)
        || /\((\d+):(\d+)\)/.exec(error.message);
    if (!match) {
        return new JavaFormatError("Syntax error: the code could not be formatted");
    }
    const line = Math.max(1, Number(match[1]) - lineOffset);
    return new JavaFormatError(`Line ${line}, column ${match[2]}: syntax error`);
}
