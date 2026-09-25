// Unit tests for the in-browser Java formatter (Prettier + prettier-plugin-java).
// Run with: node --test 'src/test/js/**/*.test.mjs'
import { test } from "node:test";
import assert from "node:assert/strict";
import { fileURLToPath } from "node:url";
import { formatJava } from "../../main/resources/static/js/editor/java-formatter.js";

// In the browser the vendored files are served from /vendor/...; here we load them from disk.
const VENDOR = new URL("../../main/resources/static/vendor/prettier-java-3.9.9-2.8.1/",
    import.meta.url).href;
const format = (code, kind) => formatJava(code, kind, { vendorBase: VENDOR });

test("formats a whole class with 4-space indentation", async () => {
    const result = await format(
        "public class Main{public static void main(String[] args){for(int i=0;i<3;i++){"
        + "if(i%2==0){System.out.println(i);}}}}", "class");

    assert.equal(result, [
        "public class Main {",
        "",
        "    public static void main(String[] args) {",
        "        for (int i = 0; i < 3; i++) {",
        "            if (i % 2 == 0) {",
        "                System.out.println(i);",
        "            }",
        "        }",
        "    }",
        "}",
        ""
    ].join("\n"));
});

test("formats method-body statements without adding a wrapper", async () => {
    const result = await format("int sum=0;\nfor(int n:numbers){sum+=n;}\nreturn sum;", "methodBody");

    assert.equal(result, [
        "int sum = 0;",
        "for (int n : numbers) {",
        "    sum += n;",
        "}",
        "return sum;",
        ""
    ].join("\n"));
});

test("is idempotent", async () => {
    const once = await format("class A{int x=1;void f(){x++;}}", "class");

    assert.equal(await format(once, "class"), once);
});

test("keeps comments", async () => {
    const result = await format("// helper\nint x=1; /* why */", "methodBody");

    assert.match(result, /\/\/ helper/);
    assert.match(result, /\/\* why \*\//);
});

test("reports syntax errors with the user's line number", async () => {
    await assert.rejects(format("class A {\n  void f() {\n    int x = ;\n  }\n}", "class"),
        (error) => error.message.startsWith("Line 3"));
    // Method bodies are wrapped internally; the line must still match the editor.
    await assert.rejects(format("int a = 1;\nint b = ;", "methodBody"),
        (error) => error.message.startsWith("Line 2"));
});

test("blank input stays empty", async () => {
    assert.equal(await format("   \n", "methodBody"), "");
});
