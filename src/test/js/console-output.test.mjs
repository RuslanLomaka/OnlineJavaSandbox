// Unit tests for the console colouring logic.
// Run with: node --test 'src/test/js/**/*.test.mjs'
import { test } from "node:test";
import assert from "node:assert/strict";
import { classifyLine, summarize } from "../../main/resources/static/js/console-output.js";

test("test results get pass / fail / error kinds", () => {
    assert.equal(classifyLine("[PASS] empty array"), "pass");
    assert.equal(classifyLine("[FAIL] all equal"), "fail");
    assert.equal(classifyLine("[ERROR] Null input"), "error");
    assert.equal(classifyLine("       Expected: [1, 2]"), "expected");
    assert.equal(classifyLine("       Actual:   [2, 1]"), "actual");
});

test("summaries and exit codes", () => {
    assert.equal(classifyLine("All tests passed!"), "success");
    assert.equal(classifyLine("Passed 7 of 7 tests."), "summary");
    assert.equal(classifyLine("Process finished with exit code 0"), "exit-ok");
    assert.equal(classifyLine("Process finished with exit code 1"), "exit-fail");
});

test("compiler errors, exceptions and stack traces", () => {
    assert.equal(classifyLine("Main.java:5: error: ';' expected"), "error");
    assert.equal(classifyLine("Main.java:9: warning: [unchecked] unchecked call"), "warning");
    assert.equal(classifyLine("Exception in thread \"main\" java.lang.ArithmeticException: / by zero"), "error");
    assert.equal(classifyLine("java.lang.NullPointerException: boom"), "error");
    assert.equal(classifyLine("\tat Main.main(Main.java:4)"), "trace");
    assert.equal(classifyLine("        ^"), "trace");
});

test("service messages", () => {
    assert.equal(classifyLine("Compiling..."), "meta");
    assert.equal(classifyLine("Running tests..."), "meta");
    assert.equal(classifyLine("Execution timed out."), "error");
    assert.equal(classifyLine("Could not reach the execution queue."), "error");
    assert.equal(classifyLine("(output truncated - exceeded 64KB limit)"), "warning");
    assert.equal(classifyLine("Hello, world!"), "plain");
});

test("summarize gives a header badge", () => {
    assert.deepEqual(summarize("[PASS] a\n\nPassed 2 of 2 tests.\nAll tests passed!\n\nProcess finished with exit code 0"),
        { state: "ok", label: "✔ Passed" });
    assert.deepEqual(summarize("[PASS] a\n[FAIL] b\n[ERROR] c\nPassed 1 of 3 tests.\nProcess finished with exit code 1"),
        { state: "fail", label: "✖ 2 failed" });
    assert.deepEqual(summarize("Hello\nProcess finished with exit code 0"), { state: "ok", label: "exit 0" });
    assert.deepEqual(summarize("Main.java:1: error: x\nProcess finished with exit code 1"),
        { state: "fail", label: "exit 1" });
    assert.deepEqual(summarize("Execution timed out."), { state: "fail", label: "Error" });
    assert.deepEqual(summarize("Waiting..."), { state: "idle", label: "" });
});
