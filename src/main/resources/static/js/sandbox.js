const javaWords = [
        "abstract",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extends",
        "false",
        "final",
        "finally",
        "float",
        "for",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "new",
        "null",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "super",
        "switch",
        "this",
        "throw",
        "throws",
        "true",
        "try",
        "void",
        "while",

        "String",
        "Integer",
        "Long",
        "Double",
        "Boolean",
        "Character",
        "Object",
        "Math",
        "System",
        "StringBuilder",
        "Scanner",

        "List",
        "ArrayList",
        "LinkedList",
        "Set",
        "HashSet",
        "Map",
        "HashMap",
        "Queue",
        "Deque",
        "Arrays",
        "Collections",

        "System.out.print",
        "System.out.println"
    ];

    const snippets = {
        sout: "System.out.println();",

        main:
            `public static void main(String[] args) {

}`,

        for:
            `for (int i = 0; i < 10; i++) {

}`,

        foreach:
            `for (var item : collection) {

}`,

        while:
            `while (condition) {

}`,

        if:
            `if (condition) {

}`,

        ifelse:
            `if (condition) {

} else {

}`,

        trycatch:
            `try {

} catch (Exception exception) {
    exception.printStackTrace();
}`
    };


    const editor = CodeMirror.fromTextArea(
        document.getElementById("code"),
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
                "Ctrl-Space": showAutocomplete,
                "Cmd-Space": showAutocomplete,

                "Ctrl-Enter": runCode,
                "Cmd-Enter": runCode,

                "Tab": function (codeMirror) {
                    if (codeMirror.somethingSelected()) {
                        codeMirror.indentSelection("add");
                    } else {
                        codeMirror.replaceSelection("    ", "end");
                    }
                }
            }
        }
    );


    function findDeclaredVariables() {
        const code = editor.getValue();

        const variablePattern =
            /\b(?:int|long|double|float|boolean|char|byte|short|String|Integer|Long|Double|Boolean|Character|Object|StringBuilder|List|ArrayList|LinkedList|Map|HashMap|Set|HashSet|Queue|Deque)\s*(?:<[^;=]+>)?\s+([a-zA-Z_$][\w$]*)/g;

        const variables = [];
        const names = new Set();

        for (const match of code.matchAll(variablePattern)) {
            const variableName = match[1];

            if (!names.has(variableName)) {
                names.add(variableName);
                variables.push(variableName);
            }
        }

        return variables;
    }


    function getCurrentWord() {
        const cursor = editor.getCursor();
        const line = editor.getLine(cursor.line);

        let start = cursor.ch;
        let end = cursor.ch;

        while (
            start > 0 &&
            /[\w.$]/.test(line.charAt(start - 1))
            ) {
            start--;
        }

        while (
            end < line.length &&
            /[\w.$]/.test(line.charAt(end))
            ) {
            end++;
        }

        return {
            word: line.slice(start, end),
            from: CodeMirror.Pos(cursor.line, start),
            to: CodeMirror.Pos(cursor.line, end)
        };
    }


    function showAutocomplete() {
        const current = getCurrentWord();

        const suggestions = [
            ...Object.keys(snippets),
            ...findDeclaredVariables(),
            ...javaWords
        ];

        const uniqueSuggestions =
            [...new Set(suggestions)];

        const filteredSuggestions =
            uniqueSuggestions
                .filter(item =>
                    item.toLowerCase().startsWith(
                        current.word.toLowerCase()
                    )
                )
                .map(item => ({
                    text: snippets[item] ?? item,
                    displayText: item
                }));

        if (filteredSuggestions.length === 0) {
            return;
        }

        editor.showHint({
            hint: function () {
                return {
                    list: filteredSuggestions,
                    from: current.from,
                    to: current.to
                };
            },

            completeSingle: false
        });
    }


    editor.on("inputRead", function (codeMirror, change) {
        if (
            change.text.length === 1 &&
            /^[a-zA-Z.]$/.test(change.text[0])
        ) {
            showAutocomplete();
        }
    });


    const runButton =
        document.getElementById("runButton");

    const consoleOutput =
        document.getElementById("console");


    async function runCode() {
        runButton.disabled = true;
        consoleOutput.textContent = "Compiling...\n";

        try {
            const response = await fetch("/sandbox/run", {
                method: "POST",

                headers: {
                    "Content-Type":
                        "text/plain;charset=UTF-8"
                },

                body: editor.getValue()
            });

            consoleOutput.textContent =
                await response.text();

        } catch (error) {
            consoleOutput.textContent =
                "Request failed:\n" + error.message;

        } finally {
            runButton.disabled = false;
        }
    }


    runButton.addEventListener("click", runCode);

    editor.focus();
