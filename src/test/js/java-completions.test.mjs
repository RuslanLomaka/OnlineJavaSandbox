// Unit tests for the editor's Java autocomplete logic.
// Run with: node --test src/test/js
import { test } from "node:test";
import assert from "node:assert/strict";
import {
    findDeclaredVariables,
    suggestionsAt,
    TEMPLATES,
    JDK_TYPES
} from "../../main/resources/static/js/editor/java-completions.js";

/** Suggestions with the cursor where "|" is in the source. */
function at(sourceWithCursor) {
    const offset = sourceWithCursor.indexOf("|");
    return suggestionsAt(sourceWithCursor.replace("|", ""), offset);
}

const labels = (result) => result.items.map((item) => item.label);

test("finds declared variables with their types", () => {
    const vars = findDeclaredVariables(`
        int count = 0;
        String name = "x";
        List<Integer> numbers = new ArrayList<>();
        Map<String, List<Integer>> index = new HashMap<>();
        int[] values = {1, 2};
        for (String word : words) {}
        void f(Deque<Character> stack, long total) {}
    `);

    assert.equal(vars.get("count"), "int");
    assert.equal(vars.get("name"), "String");
    assert.equal(vars.get("numbers"), "List");
    assert.equal(vars.get("index"), "Map");
    assert.equal(vars.get("values"), "int[]");
    assert.equal(vars.get("word"), "String");
    assert.equal(vars.get("stack"), "Deque");
    assert.equal(vars.get("total"), "long");
});

test("does not mistake keywords or method calls for declarations", () => {
    const vars = findDeclaredVariables("return value; new Foo(); System.out.println(x);");

    assert.equal(vars.has("value"), false);
    assert.equal(vars.has("out"), false);
});

test("suggests instance methods after a variable of a known type", () => {
    const result = at("List<Integer> numbers = new ArrayList<>();\nnumbers.|");

    assert.equal(result.context, "member");
    assert.ok(labels(result).includes("add"));
    assert.ok(labels(result).includes("size"));
    assert.ok(!labels(result).includes("for"), "no keywords after a dot");
});

test("suggests static members after a class name", () => {
    const result = at("int m = Math.|");

    assert.equal(result.context, "member");
    assert.ok(labels(result).includes("max"));
    assert.ok(labels(result).includes("abs"));
});

test("suggests PrintStream methods after System.out.", () => {
    assert.deepEqual(
        ["print", "printf", "println"].filter((m) => labels(at("System.out.|")).includes(m)),
        ["print", "printf", "println"]);
});

test("suggests System members after System.", () => {
    const result = labels(at("System.|"));

    assert.ok(result.includes("out"));
    assert.ok(result.includes("currentTimeMillis"));
});

test("arrays only offer length after a dot", () => {
    assert.deepEqual(labels(at("int[] values = {1};\nvalues.|")), ["length"]);
});

test("String variables get String methods including charAt", () => {
    const result = at("String s = \"abc\";\ns.ch|");

    assert.ok(labels(result).includes("charAt"));
    assert.equal(result.prefix, "ch");
});

test("general context offers keywords, templates, variables and JDK types", () => {
    const result = at("int total = 0;\n|");
    const names = labels(result);

    assert.equal(result.context, "general");
    for (const expected of ["for", "sout", "fori", "psvm", "total", "HashMap", "String"]) {
        assert.ok(names.includes(expected), `missing ${expected}`);
    }
});

test("templates use IntelliJ names and snippet placeholders", () => {
    const sout = TEMPLATES.find((t) => t.label === "sout");
    const fori = TEMPLATES.find((t) => t.label === "fori");

    assert.equal(sout.insertText, "System.out.println($0);");
    assert.match(fori.insertText, /for \(int \$\{1:i\} = 0; \$\{1:i\} < \$\{2:length\}; \$\{1:i\}\+\+\)/);
});

test("unknown receivers produce no member suggestions", () => {
    assert.deepEqual(labels(at("mystery.|")), []);
});

test("method entries carry signatures and docs", () => {
    const charAt = JDK_TYPES.String.instanceMethods.find((m) => m.name === "charAt");

    assert.equal(charAt.signature, "char charAt(int index)");
    assert.ok(charAt.doc.length > 0);
});

test("no suggestions inside string literals or comments", () => {
    assert.equal(at("String s = \"hello Math.|\";").items.length, 0);
    assert.equal(at("// call Math.|").items.length, 0);
});
