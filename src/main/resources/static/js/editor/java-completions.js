/*
 * Java autocomplete data and logic for the code editor.
 *
 * Pure functions with no Monaco or DOM dependency, so they are unit-tested
 * with `node --test src/test/js`. java-editor.js turns the results into
 * Monaco completion items.
 *
 * This is syntax-level completion (keywords, IntelliJ-style live templates,
 * variables declared in the file, and a curated catalog of common JDK types),
 * not a full Java language server.
 */

/** Java keywords and literals offered in a general (non-member) context. */
export const KEYWORDS = [
    "abstract", "boolean", "break", "byte", "case", "catch", "char", "class",
    "continue", "default", "do", "double", "else", "enum", "extends", "false",
    "final", "finally", "float", "for", "if", "implements", "import",
    "instanceof", "int", "interface", "long", "new", "null", "private",
    "protected", "public", "record", "return", "short", "static", "super",
    "switch", "this", "throw", "throws", "true", "try", "var", "void", "while",
    "yield"
];

/**
 * IntelliJ IDEA live-template names. insertText uses snippet syntax:
 * ${1:name} is a placeholder, $0 the final cursor position.
 */
export const TEMPLATES = [
    { label: "sout", detail: "System.out.println()", insertText: "System.out.println($0);" },
    { label: "soutv", detail: "Print a variable with its name",
        insertText: "System.out.println(\"${1:value} = \" + ${1:value});$0" },
    { label: "psvm", detail: "main method",
        insertText: "public static void main(String[] args) {\n\t$0\n}" },
    { label: "main", detail: "main method",
        insertText: "public static void main(String[] args) {\n\t$0\n}" },
    { label: "fori", detail: "Indexed for loop",
        insertText: "for (int ${1:i} = 0; ${1:i} < ${2:length}; ${1:i}++) {\n\t$0\n}" },
    { label: "iter", detail: "Enhanced for loop",
        insertText: "for (${1:var} ${2:item} : ${3:items}) {\n\t$0\n}" },
    { label: "ifn", detail: "if (x == null)",
        insertText: "if (${1:value} == null) {\n\t$0\n}" },
    { label: "inn", detail: "if (x != null)",
        insertText: "if (${1:value} != null) {\n\t$0\n}" },
    { label: "thr", detail: "throw new ...",
        insertText: "throw new ${1:IllegalArgumentException}(\"$0\");" },
    { label: "trycatch", detail: "try / catch",
        insertText: "try {\n\t$0\n} catch (${1:Exception} ${2:e}) {\n\t${2:e}.printStackTrace();\n}" },
    { label: "whilet", detail: "while (true) loop",
        insertText: "while (${1:true}) {\n\t$0\n}" }
];

/** Shorthand for method catalog entries: [name, signature, doc]. */
function methods(list) {
    return list.map(([name, signature, doc]) => ({ name, signature, doc }));
}

const OBJECT_METHODS = methods([
    ["equals", "boolean equals(Object other)", "Whether this object is equal to another."],
    ["hashCode", "int hashCode()", "Hash code consistent with equals."],
    ["toString", "String toString()", "String representation."]
]);

const COLLECTION_METHODS = methods([
    ["add", "boolean add(E element)", "Adds an element."],
    ["addAll", "boolean addAll(Collection<? extends E> c)", "Adds all elements of c."],
    ["clear", "void clear()", "Removes all elements."],
    ["contains", "boolean contains(Object o)", "Whether the element is present."],
    ["isEmpty", "boolean isEmpty()", "Whether there are no elements."],
    ["remove", "boolean remove(Object o)", "Removes one occurrence of the element."],
    ["size", "int size()", "Number of elements."],
    ["stream", "Stream<E> stream()", "Sequential stream over the elements."],
    ["forEach", "void forEach(Consumer<? super E> action)", "Runs action for each element."],
    ["toArray", "Object[] toArray()", "Copies the elements into an array."]
]);

const LIST_METHODS = COLLECTION_METHODS.concat(methods([
    ["get", "E get(int index)", "Element at index."],
    ["set", "E set(int index, E element)", "Replaces the element at index."],
    ["indexOf", "int indexOf(Object o)", "First index of o, or -1."],
    ["lastIndexOf", "int lastIndexOf(Object o)", "Last index of o, or -1."],
    ["sort", "void sort(Comparator<? super E> c)", "Sorts in place (null = natural order)."],
    ["subList", "List<E> subList(int from, int to)", "View of the range [from, to)."]
]));

const DEQUE_METHODS = COLLECTION_METHODS.concat(methods([
    ["push", "void push(E e)", "Pushes onto the front (stack)."],
    ["pop", "E pop()", "Removes and returns the front (stack)."],
    ["peek", "E peek()", "Front element, or null if empty."],
    ["offer", "boolean offer(E e)", "Adds at the back (queue)."],
    ["poll", "E poll()", "Removes and returns the front, or null."],
    ["addFirst", "void addFirst(E e)", "Inserts at the front."],
    ["addLast", "void addLast(E e)", "Inserts at the back."],
    ["pollFirst", "E pollFirst()", "Removes the first element, or null."],
    ["pollLast", "E pollLast()", "Removes the last element, or null."],
    ["peekFirst", "E peekFirst()", "First element, or null."],
    ["peekLast", "E peekLast()", "Last element, or null."]
]));

const QUEUE_METHODS = COLLECTION_METHODS.concat(methods([
    ["offer", "boolean offer(E e)", "Adds an element."],
    ["poll", "E poll()", "Removes and returns the head, or null."],
    ["peek", "E peek()", "Head element, or null."]
]));

const MAP_METHODS = methods([
    ["put", "V put(K key, V value)", "Associates value with key."],
    ["get", "V get(Object key)", "Value for key, or null."],
    ["getOrDefault", "V getOrDefault(Object key, V defaultValue)", "Value for key, or the default."],
    ["containsKey", "boolean containsKey(Object key)", "Whether the key is present."],
    ["containsValue", "boolean containsValue(Object value)", "Whether some key maps to value."],
    ["remove", "V remove(Object key)", "Removes the mapping for key."],
    ["putIfAbsent", "V putIfAbsent(K key, V value)", "Puts only if the key is absent."],
    ["merge", "V merge(K key, V value, BiFunction<V, V, V> fn)", "Combines with an existing value, e.g. counting."],
    ["computeIfAbsent", "V computeIfAbsent(K key, Function<K, V> fn)", "Creates the value on first access."],
    ["keySet", "Set<K> keySet()", "View of the keys."],
    ["values", "Collection<V> values()", "View of the values."],
    ["entrySet", "Set<Map.Entry<K, V>> entrySet()", "View of the key-value pairs."],
    ["size", "int size()", "Number of mappings."],
    ["isEmpty", "boolean isEmpty()", "Whether there are no mappings."],
    ["clear", "void clear()", "Removes all mappings."],
    ["forEach", "void forEach(BiConsumer<K, V> action)", "Runs action for each mapping."]
]);

/**
 * Curated catalog of common JDK types: instance methods (after `variable.`),
 * static methods and static fields (after `TypeName.`). Static fields with a
 * `type` can be completed further, e.g. `System.out.` -> PrintStream.
 */
export const JDK_TYPES = {
    Object: { instanceMethods: OBJECT_METHODS },
    String: {
        instanceMethods: OBJECT_METHODS.concat(methods([
            ["charAt", "char charAt(int index)", "Character at index."],
            ["length", "int length()", "Number of chars."],
            ["isEmpty", "boolean isEmpty()", "Whether length() is 0."],
            ["isBlank", "boolean isBlank()", "Whether empty or only whitespace."],
            ["substring", "String substring(int begin, int end)", "Characters in [begin, end)."],
            ["indexOf", "int indexOf(String str)", "First index of str, or -1."],
            ["lastIndexOf", "int lastIndexOf(String str)", "Last index of str, or -1."],
            ["contains", "boolean contains(CharSequence s)", "Whether s occurs in this string."],
            ["startsWith", "boolean startsWith(String prefix)", "Whether this starts with prefix."],
            ["endsWith", "boolean endsWith(String suffix)", "Whether this ends with suffix."],
            ["equalsIgnoreCase", "boolean equalsIgnoreCase(String other)", "Case-insensitive equality."],
            ["compareTo", "int compareTo(String other)", "Lexicographic comparison."],
            ["toCharArray", "char[] toCharArray()", "Copies the chars into a new array."],
            ["toLowerCase", "String toLowerCase()", "Lower-case copy."],
            ["toUpperCase", "String toUpperCase()", "Upper-case copy."],
            ["trim", "String trim()", "Copy without leading/trailing spaces."],
            ["strip", "String strip()", "Copy without leading/trailing whitespace."],
            ["split", "String[] split(String regex)", "Splits around matches of regex."],
            ["replace", "String replace(CharSequence target, CharSequence replacement)", "Replaces every occurrence."],
            ["repeat", "String repeat(int count)", "This string repeated count times."],
            ["chars", "IntStream chars()", "Stream of the chars as ints."]
        ])),
        staticMethods: methods([
            ["valueOf", "static String valueOf(Object obj)", "String form of obj (\"null\" for null)."],
            ["join", "static String join(CharSequence delimiter, Iterable<?> elements)", "Joins elements with delimiter."],
            ["format", "static String format(String format, Object... args)", "printf-style formatting."]
        ])
    },
    StringBuilder: {
        instanceMethods: OBJECT_METHODS.concat(methods([
            ["append", "StringBuilder append(Object value)", "Appends value's string form."],
            ["insert", "StringBuilder insert(int offset, Object value)", "Inserts at offset."],
            ["charAt", "char charAt(int index)", "Character at index."],
            ["setCharAt", "void setCharAt(int index, char c)", "Replaces the char at index."],
            ["deleteCharAt", "StringBuilder deleteCharAt(int index)", "Removes the char at index."],
            ["length", "int length()", "Number of chars."],
            ["reverse", "StringBuilder reverse()", "Reverses the contents in place."],
            ["setLength", "void setLength(int length)", "Truncates or pads with \\0."]
        ]))
    },
    Math: {
        staticMethods: methods([
            ["abs", "static int abs(int a)", "Absolute value."],
            ["max", "static int max(int a, int b)", "Larger of two values."],
            ["min", "static int min(int a, int b)", "Smaller of two values."],
            ["pow", "static double pow(double a, double b)", "a raised to the power b."],
            ["sqrt", "static double sqrt(double a)", "Square root."],
            ["floor", "static double floor(double a)", "Largest integer value <= a."],
            ["ceil", "static double ceil(double a)", "Smallest integer value >= a."],
            ["round", "static long round(double a)", "Nearest whole number."],
            ["random", "static double random()", "Random value in [0.0, 1.0)."],
            ["floorMod", "static int floorMod(int x, int y)", "Modulo that is never negative for y > 0."]
        ]),
        staticFields: [{ name: "PI", type: "double", doc: "The ratio of a circle's circumference to its diameter." }]
    },
    Integer: {
        instanceMethods: OBJECT_METHODS.concat(methods([
            ["intValue", "int intValue()", "Value as an int."],
            ["compareTo", "int compareTo(Integer other)", "Numeric comparison."]
        ])),
        staticMethods: methods([
            ["parseInt", "static int parseInt(String s)", "Parses a decimal int."],
            ["valueOf", "static Integer valueOf(int i)", "Boxed value (cached for small numbers)."],
            ["compare", "static int compare(int x, int y)", "Negative, zero or positive."],
            ["toBinaryString", "static String toBinaryString(int i)", "Unsigned binary digits."],
            ["bitCount", "static int bitCount(int i)", "Number of one-bits."]
        ]),
        staticFields: [
            { name: "MAX_VALUE", type: "int", doc: "2^31 - 1" },
            { name: "MIN_VALUE", type: "int", doc: "-2^31" }
        ]
    },
    Character: {
        staticMethods: methods([
            ["isDigit", "static boolean isDigit(char c)", "Whether c is a digit."],
            ["isLetter", "static boolean isLetter(char c)", "Whether c is a letter."],
            ["isLetterOrDigit", "static boolean isLetterOrDigit(char c)", "Whether c is a letter or digit."],
            ["isWhitespace", "static boolean isWhitespace(char c)", "Whether c is whitespace."],
            ["isUpperCase", "static boolean isUpperCase(char c)", "Whether c is upper case."],
            ["isLowerCase", "static boolean isLowerCase(char c)", "Whether c is lower case."],
            ["toUpperCase", "static char toUpperCase(char c)", "Upper-case version of c."],
            ["toLowerCase", "static char toLowerCase(char c)", "Lower-case version of c."],
            ["getNumericValue", "static int getNumericValue(char c)", "Digit value, e.g. '7' -> 7."]
        ])
    },
    Arrays: {
        staticMethods: methods([
            ["sort", "static void sort(int[] a)", "Sorts the array in ascending order."],
            ["toString", "static String toString(int[] a)", "Readable form like [1, 2, 3]."],
            ["fill", "static void fill(int[] a, int value)", "Sets every element to value."],
            ["copyOf", "static int[] copyOf(int[] original, int newLength)", "Copy truncated or padded."],
            ["copyOfRange", "static int[] copyOfRange(int[] a, int from, int to)", "Copy of [from, to)."],
            ["equals", "static boolean equals(int[] a, int[] b)", "Element-wise equality."],
            ["asList", "static <T> List<T> asList(T... values)", "Fixed-size list view."],
            ["stream", "static IntStream stream(int[] a)", "Stream over the array."],
            ["binarySearch", "static int binarySearch(int[] a, int key)", "Index of key in a sorted array."]
        ])
    },
    Collections: {
        staticMethods: methods([
            ["sort", "static <T> void sort(List<T> list)", "Sorts the list in natural order."],
            ["reverse", "static void reverse(List<?> list)", "Reverses the list in place."],
            ["max", "static <T> T max(Collection<T> c)", "Largest element."],
            ["min", "static <T> T min(Collection<T> c)", "Smallest element."],
            ["swap", "static void swap(List<?> list, int i, int j)", "Swaps two elements."],
            ["emptyList", "static <T> List<T> emptyList()", "Immutable empty list."],
            ["unmodifiableList", "static <T> List<T> unmodifiableList(List<T> list)", "Read-only view."],
            ["frequency", "static int frequency(Collection<?> c, Object o)", "How often o occurs."]
        ])
    },
    List: {
        instanceMethods: LIST_METHODS,
        staticMethods: methods([["of", "static <E> List<E> of(E... elements)", "Immutable list."]])
    },
    ArrayList: { instanceMethods: LIST_METHODS },
    LinkedList: { instanceMethods: LIST_METHODS.concat(DEQUE_METHODS) },
    Set: {
        instanceMethods: COLLECTION_METHODS,
        staticMethods: methods([["of", "static <E> Set<E> of(E... elements)", "Immutable set."]])
    },
    HashSet: { instanceMethods: COLLECTION_METHODS },
    TreeSet: {
        instanceMethods: COLLECTION_METHODS.concat(methods([
            ["first", "E first()", "Smallest element."],
            ["last", "E last()", "Largest element."],
            ["floor", "E floor(E e)", "Largest element <= e, or null."],
            ["ceiling", "E ceiling(E e)", "Smallest element >= e, or null."]
        ]))
    },
    Map: {
        instanceMethods: MAP_METHODS,
        staticMethods: methods([["of", "static <K, V> Map<K, V> of(K k1, V v1, ...)", "Immutable map."]])
    },
    HashMap: { instanceMethods: MAP_METHODS },
    TreeMap: {
        instanceMethods: MAP_METHODS.concat(methods([
            ["firstKey", "K firstKey()", "Smallest key."],
            ["lastKey", "K lastKey()", "Largest key."],
            ["floorKey", "K floorKey(K key)", "Largest key <= key, or null."],
            ["ceilingKey", "K ceilingKey(K key)", "Smallest key >= key, or null."]
        ]))
    },
    Deque: { instanceMethods: DEQUE_METHODS },
    ArrayDeque: { instanceMethods: DEQUE_METHODS },
    Queue: { instanceMethods: QUEUE_METHODS },
    PriorityQueue: { instanceMethods: QUEUE_METHODS },
    Objects: {
        staticMethods: methods([
            ["equals", "static boolean equals(Object a, Object b)", "Null-safe equality."],
            ["hash", "static int hash(Object... values)", "Hash code of several values."],
            ["requireNonNull", "static <T> T requireNonNull(T obj)", "Throws if obj is null."],
            ["isNull", "static boolean isNull(Object obj)", "Whether obj is null."]
        ])
    },
    System: {
        staticMethods: methods([
            ["currentTimeMillis", "static long currentTimeMillis()", "Wall-clock time in milliseconds."],
            ["nanoTime", "static long nanoTime()", "High-resolution time for measuring durations."],
            ["arraycopy", "static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length)", "Copies part of an array."],
            ["exit", "static void exit(int status)", "Stops the program."]
        ]),
        staticFields: [
            { name: "out", type: "PrintStream", doc: "Standard output." },
            { name: "err", type: "PrintStream", doc: "Standard error." },
            { name: "in", type: "InputStream", doc: "Standard input." }
        ]
    },
    PrintStream: {
        instanceMethods: methods([
            ["println", "void println(Object x)", "Prints x and a line break."],
            ["print", "void print(Object x)", "Prints x."],
            ["printf", "PrintStream printf(String format, Object... args)", "Formatted output, e.g. %d, %s, %n."]
        ])
    }
};

const PRIMITIVES = new Set(["int", "long", "double", "float", "boolean", "char", "byte", "short"]);

const NOT_A_TYPE = new Set(["return", "new", "throw", "else", "case", "import", "package", "yield"]);

/**
 * Finds variables, fields, parameters and for-each variables declared in the
 * code, mapped to their type without generic arguments (`List<Integer>` ->
 * `List`; arrays keep their brackets, `int[]`).
 *
 * @param {string} code source code
 * @returns {Map<string, string>} variable name -> type
 */
export function findDeclaredVariables(code) {
    const variables = new Map();
    const declaration =
        /\b([A-Z][\w$]*|int|long|double|float|boolean|char|byte|short)\s*(<[^;=(){}]*>)?\s*((?:\[\s*\])*)\s+([a-zA-Z_$][\w$]*)\s*(?=[=;,:)])/g;
    for (const match of stripCommentsAndStrings(code).matchAll(declaration)) {
        const [, type, , brackets, name] = match;
        if (!NOT_A_TYPE.has(type) && !variables.has(name)) {
            variables.set(name, type + (brackets ? brackets.replace(/\s+/g, "") : ""));
        }
    }
    return variables;
}

/**
 * Computes completion suggestions for a cursor position.
 *
 * @param {string} code full editor text
 * @param {number} offset cursor offset in code
 * @returns {{context: string, prefix: string, items: Array<object>}}
 *   `context` is "member" (after a dot), "general", or "none" (inside a
 *   string or comment); `prefix` is the partial word being typed.
 */
export function suggestionsAt(code, offset) {
    const before = code.slice(0, offset);
    if (isInsideStringOrComment(before)) {
        return { context: "none", prefix: "", items: [] };
    }

    const member = /((?:[A-Za-z_$][\w$]*\s*\.\s*)+)([A-Za-z_$][\w$]*)?$/.exec(before);
    if (member) {
        const chain = member[1].split(".").map((part) => part.trim()).filter(Boolean);
        return {
            context: "member",
            prefix: member[2] || "",
            items: memberItems(chain, findDeclaredVariables(code))
        };
    }

    const prefix = (/[A-Za-z_$][\w$]*$/.exec(before) || [""])[0];
    return { context: "general", prefix, items: generalItems(findDeclaredVariables(code)) };
}

function generalItems(variables) {
    return [
        ...[...variables].map(([name, type]) => ({
            label: name, kind: "variable", insertText: name, detail: type
        })),
        ...TEMPLATES.map((t) => ({
            label: t.label, kind: "snippet", insertText: t.insertText, isSnippet: true, detail: t.detail
        })),
        ...KEYWORDS.map((word) => ({ label: word, kind: "keyword", insertText: word })),
        ...Object.keys(JDK_TYPES).map((type) => ({
            label: type, kind: "class", insertText: type, detail: "java class"
        }))
    ];
}

/** Resolves a chain like ["System", "out"] or ["numbers"] to member items. */
function memberItems(chain, variables) {
    let type = null;
    let isStatic = false;

    const [head, ...rest] = chain;
    if (variables.has(head)) {
        type = variables.get(head);
    } else if (JDK_TYPES[head]) {
        type = head;
        isStatic = true;
    } else {
        return [];
    }

    for (const field of rest) {
        const entry = isStatic ? (JDK_TYPES[type]?.staticFields || []).find((f) => f.name === field) : null;
        if (!entry) {
            return [];
        }
        type = entry.type;
        isStatic = false;
    }

    if (type.endsWith("[]")) {
        return [{ label: "length", kind: "field", insertText: "length", detail: "int length" }];
    }
    if (PRIMITIVES.has(type)) {
        return [];
    }
    const catalog = JDK_TYPES[type];
    if (!catalog) {
        return [];
    }
    if (isStatic) {
        return [
            ...(catalog.staticFields || []).map((f) => ({
                label: f.name, kind: "field", insertText: f.name, detail: `${f.type} ${f.name}`, documentation: f.doc
            })),
            ...(catalog.staticMethods || []).map(methodItem)
        ];
    }
    return (catalog.instanceMethods || []).map(methodItem);
}

function methodItem(method) {
    const takesArguments = !/\(\s*\)/.test(method.signature);
    return {
        label: method.name,
        kind: "method",
        insertText: takesArguments ? `${method.name}($0)` : `${method.name}()`,
        isSnippet: true,
        detail: method.signature,
        documentation: method.doc
    };
}

/** Whether the cursor (end of `before`) is inside a string, char literal or comment. */
function isInsideStringOrComment(before) {
    let inString = null;
    let inBlockComment = false;
    for (let i = 0; i < before.length; i++) {
        const c = before[i];
        const next = before[i + 1];
        if (inBlockComment) {
            if (c === "*" && next === "/") {
                inBlockComment = false;
                i++;
            }
        } else if (inString) {
            if (c === "\\") {
                i++;
            } else if (c === inString || c === "\n") {
                inString = null;
            }
        } else if (c === "/" && next === "/") {
            const end = before.indexOf("\n", i);
            if (end === -1) {
                return true;
            }
            i = end;
        } else if (c === "/" && next === "*") {
            inBlockComment = true;
            i++;
        } else if (c === "\"" || c === "'") {
            inString = c;
        }
    }
    return inString !== null || inBlockComment;
}

/** Replaces comment and string contents with spaces so regexes ignore them. */
function stripCommentsAndStrings(code) {
    return code
        .replace(/\/\*[\s\S]*?\*\//g, (m) => " ".repeat(m.length))
        .replace(/\/\/[^\n]*/g, (m) => " ".repeat(m.length))
        .replace(/"(?:\\.|[^"\\\n])*"/g, (m) => "\"" + " ".repeat(m.length - 2) + "\"");
}
