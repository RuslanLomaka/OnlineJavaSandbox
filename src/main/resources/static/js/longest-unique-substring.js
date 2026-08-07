const editor = CodeMirror.fromTextArea(
        document.getElementById("solutionCode"),
        {
            mode: "text/x-java",
            theme: "material-darker",

            lineNumbers: true,
            lineWrapping: true,

            indentUnit: 4,
            tabSize: 4,
            indentWithTabs: false,

            autoCloseBrackets: true,
            matchBrackets: true,

            extraKeys: {
                "Ctrl-Enter": runTests,
                "Cmd-Enter": runTests,

                "Tab": function (codeMirror) {
                    if (codeMirror.somethingSelected()) {
                        codeMirror.indentSelection("add");
                    } else {
                        codeMirror.replaceSelection(
                            "    ",
                            "end"
                        );
                    }
                }
            }
        }
    );

    const runTestsButton =
        document.getElementById("runTestsButton");

    const testConsole =
        document.getElementById("testConsole");


    function createCompleteJavaSource(solutionBody) {
        return `import java.util.HashMap;
import java.util.Map;

public class Main {

    public static int longestUniqueLength(String text) {
${solutionBody}
    }

    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        runTest(
                1,
                "Typical repeated sequence",
                "abcabcbb",
                3
        );

        runTest(
                2,
                "Every character is the same",
                "bbbbb",
                1
        );

        runTest(
                3,
                "Window moves past earlier repeat",
                "pwwkew",
                3
        );

        runTest(
                4,
                "Empty string",
                "",
                0
        );

        runTest(
                5,
                "Null input",
                null,
                0
        );

        runTest(
                6,
                "One character",
                "x",
                1
        );

        runTest(
                7,
                "All characters are unique",
                "abcdef",
                6
        );

        runTest(
                8,
                "Repeat after unique prefix",
                "abcddef",
                4
        );

        runTest(
                9,
                "Repeat inside current window",
                "abba",
                2
        );

        runTest(
                10,
                "Spaces count as characters",
                "a b c a",
                3
        );

        runTest(
                11,
                "Uppercase and lowercase differ",
                "aAbBcA",
                5
        );

        runTest(
                12,
                "Digits and punctuation",
                "1!2@1#",
                5
        );

        runTest(
                13,
                "Longest window occurs at end",
                "aabcde",
                5
        );

        runTest(
                14,
                "Window must not move backward",
                "tmmzuxt",
                5
        );

        runTest(
                15,
                "Non-ASCII Java chars",
                "你好吗你很",
                4
        );

        runPerformanceSmokeTest(
                16,
                "Large-input performance smoke test"
        );

        runTest(
                17,
                "Do not discard reusable characters",
                "abaca",
                3
        );

        runTest(
                18,
                "Repeated char before current window",
                "dvdf",
                3
        );

        runTest(
                19,
                "Unique suffix after repetitions",
                "aaaaabcdef",
                6
        );

        runTest(
                20,
                "Whitespace variations",
                "a\\tb a",
                4
        );

        printSummary();

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    private static void runTest(
            int number,
            String description,
            String input,
            int expected
    ) {
        try {
            int actual = longestUniqueLength(input);

            if (actual == expected) {
                passedTests++;

                System.out.printf(
                        "[PASS] Test %02d: %s%n",
                        number,
                        description
                );

            } else {
                failedTests++;

                System.out.printf(
                        "[FAIL] Test %02d: %s%n",
                        number,
                        description
                );

                System.out.println(
                        "       Expected: " + expected
                );

                System.out.println(
                        "       Actual:   " + actual
                );
            }

        } catch (Throwable error) {
            failedTests++;

            System.out.printf(
                    "[ERROR] Test %02d: %s%n",
                    number,
                    description
            );

            System.out.printf(
                    "        %s: %s%n",
                    error.getClass().getSimpleName(),
                    readableMessage(error)
            );
        }
    }

    private static void runPerformanceSmokeTest(
            int number,
            String description
    ) {
        final int inputLength = 30_000;
        final long maximumExpectedMillis = 2_000;

        StringBuilder input =
                new StringBuilder(inputLength);

        for (
                int character = 1_000;
                character < 1_000 + inputLength;
                character++
        ) {
            input.append((char) character);
        }

        long started = System.nanoTime();

        final int actual;

        try {
            actual =
                    longestUniqueLength(input.toString());

        } catch (Throwable error) {
            failedTests++;

            System.out.printf(
                    "[ERROR] Test %02d: %s%n",
                    number,
                    description
            );

            System.out.printf(
                    "        %s: %s%n",
                    error.getClass().getSimpleName(),
                    readableMessage(error)
            );

            return;
        }

        long elapsedMillis =
                (System.nanoTime() - started)
                        / 1_000_000;

        boolean correct =
                actual == inputLength;

        boolean reasonablyFast =
                elapsedMillis < maximumExpectedMillis;

        if (correct && reasonablyFast) {
            passedTests++;

            System.out.printf(
                    "[PASS] Test %02d: %s (%d ms)%n",
                    number,
                    description,
                    elapsedMillis
            );

        } else {
            failedTests++;

            System.out.printf(
                    "[FAIL] Test %02d: %s%n",
                    number,
                    description
            );

            System.out.println(
                    "       Expected: " + inputLength
            );

            System.out.println(
                    "       Actual:   " + actual
            );

            System.out.println(
                    "       Time:     "
                            + elapsedMillis
                            + " ms"
            );

            if (correct && !reasonablyFast) {
                System.out.println(
                        "       Correct result, but execution "
                                + "exceeded "
                                + maximumExpectedMillis
                                + " ms."
                );

                System.out.println(
                        "       Check whether the algorithm "
                                + "repeatedly rescans characters."
                );
            }
        }
    }

    private static void printSummary() {
        int totalTests =
                passedTests + failedTests;

        System.out.println();
        System.out.println(
                "===================================="
        );

        System.out.println("TEST SUMMARY");

        System.out.println(
                "===================================="
        );

        System.out.println(
                "PASSED: " + passedTests
        );

        System.out.println(
                "FAILED: " + failedTests
        );

        System.out.println(
                "TOTAL:  " + totalTests
        );

        System.out.println(
                "===================================="
        );

        if (failedTests == 0) {
            System.out.println(
                    "All smoke tests passed!"
            );
        }
    }

    private static String readableMessage(
            Throwable error
    ) {
        return error.getMessage() == null
                ? "(no message)"
                : error.getMessage();
    }
}`;
    }


    function escapeHtml(text) {
        return text
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;");
    }


    function formatConsoleOutput(output) {
        return output
            .split("\n")
            .map(line => {
                const safeLine =
                    escapeHtml(line);

                if (line.startsWith("[PASS]")) {
                    return `<span class="console-pass">${safeLine}</span>`;
                }

                if (line.startsWith("[FAIL]")) {
                    return `<span class="console-fail">${safeLine}</span>`;
                }

                if (
                    line.trimStart()
                        .startsWith("Expected:")
                ) {
                    return `<span class="console-expected">${safeLine}</span>`;
                }

                if (
                    line.trimStart()
                        .startsWith("Actual:")
                ) {
                    return `<span class="console-actual">${safeLine}</span>`;
                }

                if (
                    line.includes("Main.java:")
                    || line.includes("error:")
                    || line.includes("errors")
                ) {
                    return `<span class="console-compile-error">${safeLine}</span>`;
                }

                if (
                    line.startsWith("[ERROR]")
                    || line.includes("Exception")
                    || line.includes("OutOfMemoryError")
                    || line.includes("StackOverflowError")
                    || line.includes("RuntimeException")
                ) {
                    return `<span class="console-runtime-error">${safeLine}</span>`;
                }

                if (
                    line.trimStart()
                        .startsWith("at ")
                ) {
                    return `<span class="console-stack-trace">${safeLine}</span>`;
                }

                if (
                    line.startsWith("PASSED:")
                    || line.startsWith("FAILED:")
                    || line.startsWith("TOTAL:")
                    || line.includes("All smoke tests passed")
                    || line.includes("TEST SUMMARY")
                ) {
                    return `<span class="console-summary">${safeLine}</span>`;
                }

                if (
                    line.includes(
                        "Process finished with exit code 0"
                    )
                ) {
                    return `<span class="console-exit-success">${safeLine}</span>`;
                }

                if (
                    line.includes(
                        "Process finished with exit code"
                    )
                ) {
                    return `<span class="console-exit-failure">${safeLine}</span>`;
                }

                return `<span class="console-information">${safeLine}</span>`;
            })
            .join("\n");
    }


    async function runTests() {
        const solutionBody =
            editor.getValue();

        runTestsButton.disabled = true;

        testConsole.textContent =
            "Compiling and running tests...\n";

        const completeSource =
            createCompleteJavaSource(solutionBody);

        try {
            const response = await fetch(
                "/sandbox/run",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "text/plain;charset=UTF-8"
                    },

                    body: completeSource
                }
            );

            const output =
                await response.text();

            testConsole.innerHTML =
                formatConsoleOutput(output);

        } catch (error) {
            testConsole.innerHTML =
                `<span class="console-runtime-error">${
                    escapeHtml(
                        "Request failed:\n"
                        + error.message
                    )
                }</span>`;

        } finally {
            runTestsButton.disabled = false;
        }
    }


    runTestsButton.addEventListener(
        "click",
        runTests
    );

    editor.focus();
