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
        return `import java.util.Arrays;

public class Main {

    public static void bubbleSort(int[] numbers) {
${solutionBody}
    }

    private static int passedTests = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        test(
                "empty array",
                new int[]{},
                new int[]{}
        );

        test(
                "one value",
                new int[]{7},
                new int[]{7}
        );

        test(
                "already sorted",
                new int[]{1, 2, 3, 4, 5},
                new int[]{1, 2, 3, 4, 5}
        );

        test(
                "reverse sorted",
                new int[]{5, 4, 3, 2, 1},
                new int[]{1, 2, 3, 4, 5}
        );

        test(
                "duplicates",
                new int[]{4, 2, 4, 1, 2},
                new int[]{1, 2, 2, 4, 4}
        );

        test(
                "negative values",
                new int[]{-2, 5, 0, -8, 3},
                new int[]{-8, -2, 0, 3, 5}
        );

        test(
                "all equal",
                new int[]{6, 6, 6, 6},
                new int[]{6, 6, 6, 6}
        );

        System.out.println();
        System.out.println(
                "Passed "
                        + passedTests
                        + " of "
                        + totalTests
                        + " tests."
        );

        if (passedTests == totalTests) {
            System.out.println(
                    "All tests passed!"
            );
        } else {
            System.exit(1);
        }
    }

    private static void test(
            String name,
            int[] input,
            int[] expected
    ) {
        totalTests++;

        int[] originalReference = input;

        try {
            bubbleSort(input);

            boolean correctResult =
                    Arrays.equals(input, expected);

            boolean sameArray =
                    input == originalReference;

            if (correctResult && sameArray) {
                passedTests++;

                System.out.println(
                        "[PASS] " + name
                );

            } else {
                System.out.println(
                        "[FAIL] " + name
                );

                System.out.println(
                        "       Expected: "
                                + Arrays.toString(expected)
                );

                System.out.println(
                        "       Actual:   "
                                + Arrays.toString(input)
                );
            }

        } catch (Exception exception) {
            System.out.println(
                    "[ERROR] " + name
            );

            System.out.println(
                    "        "
                            + exception.getClass()
                                       .getSimpleName()
                            + ": "
                            + exception.getMessage()
            );
        }
    }
}`;
    }


    function containsForbiddenCode(solutionBody) {
        return solutionBody.includes("Arrays.sort")
            || solutionBody.includes("Collections.sort")
            || solutionBody.includes(".sorted()");
    }


    async function runTests() {
        const solutionBody = editor.getValue();

        if (containsForbiddenCode(solutionBody)) {
            testConsole.textContent =
                "Built-in sorting methods are not allowed.\n"
                + "Please implement Bubble Sort yourself.";

            return;
        }

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

            testConsole.textContent =
                await response.text();

        } catch (error) {
            testConsole.textContent =
                "Request failed:\n"
                + error.message;

        } finally {
            runTestsButton.disabled = false;
        }
    }


    runTestsButton.addEventListener(
        "click",
        runTests
    );

    editor.focus();
