package com.example.onlinejava.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.springframework.stereotype.Component;

/**
 * "Optimize imports" (IntelliJ's Ctrl+Alt+O) for a single source file.
 *
 * <p>Works on syntax only, without a classpath:
 * <ul>
 *   <li>removes single-type and static imports whose simple name is never
 *       used (wildcard imports are kept);</li>
 *   <li>adds imports for common JDK types that are used but not imported
 *       ({@link #KNOWN_TYPES}), skipping {@code java.lang}, types declared in
 *       the file, and types covered by a wildcard import;</li>
 *   <li>sorts imports in IntelliJ's default layout: other packages,
 *       blank line, {@code javax.*}, {@code java.*}, blank line, static
 *       imports.</li>
 * </ul>
 * Only the import block is rewritten; the rest of the file is untouched.
 */
@Component
public class ImportOrganizer {

  /**
   * JDK types learners commonly use, by simple name. A name that is used but
   * neither imported nor declared in the file is imported from here.
   */
  static final Map<String, String> KNOWN_TYPES = Stream.of(
          packageTypes("java.util", "ArrayDeque", "ArrayList", "Arrays", "BitSet",
              "Collection", "Collections", "Comparator", "Deque", "EnumMap", "EnumSet",
              "HashMap", "HashSet", "Iterator", "LinkedHashMap", "LinkedHashSet",
              "LinkedList", "List", "ListIterator", "Map", "NavigableMap", "NavigableSet",
              "NoSuchElementException", "Objects", "Optional", "OptionalInt", "PriorityQueue",
              "Queue", "Random", "Scanner", "Set", "SortedMap", "SortedSet", "Stack",
              "StringJoiner", "TreeMap", "TreeSet"),
          packageTypes("java.util.function", "BiFunction", "BinaryOperator", "Consumer",
              "Function", "IntBinaryOperator", "IntFunction", "IntPredicate",
              "IntUnaryOperator", "Predicate", "Supplier", "UnaryOperator"),
          packageTypes("java.util.stream", "Collectors", "IntStream", "LongStream",
              "Stream"),
          packageTypes("java.util.concurrent", "ConcurrentHashMap", "TimeUnit"),
          packageTypes("java.util.concurrent.atomic", "AtomicInteger", "AtomicLong"),
          packageTypes("java.math", "BigDecimal", "BigInteger", "RoundingMode"),
          packageTypes("java.io", "BufferedReader", "File", "IOException",
              "InputStreamReader", "PrintWriter", "UncheckedIOException"),
          packageTypes("java.nio.file", "Files", "Path", "Paths"),
          packageTypes("java.time", "Duration", "Instant", "LocalDate", "LocalDateTime"))
      .flatMap(map -> map.entrySet().stream())
      .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

  /**
   * Organizes the imports of a compilation unit.
   *
   * @param source a whole Java file
   * @return the file with its import block rewritten (unchanged if nothing
   *     needs to change)
   * @throws UnformattableCodeException if the code has syntax errors
   */
  public String organize(final String source) {
    final CompilationUnit unit = JavaSyntax.parseOrThrow(source, SourceKind.CLASS);
    final Set<String> usedNames = usedSimpleNames(unit);
    final Set<String> declaredTypes = declaredTypeNames(unit);

    final Set<String> regular = new TreeSet<>();
    final Set<String> statics = new TreeSet<>();
    final Set<String> wildcardPackages = new HashSet<>();
    final Set<String> importedNames = new HashSet<>();
    for (final Object item : unit.imports()) {
      final ImportDeclaration declaration = (ImportDeclaration) item;
      final String name = declaration.getName().getFullyQualifiedName();
      if (declaration.isOnDemand()) {
        (declaration.isStatic() ? statics : regular).add(name + ".*");
        if (!declaration.isStatic()) {
          wildcardPackages.add(name);
        }
        continue;
      }
      final String simpleName = name.substring(name.lastIndexOf('.') + 1);
      if (usedNames.contains(simpleName)) {
        (declaration.isStatic() ? statics : regular).add(name);
        importedNames.add(simpleName);
      }
    }

    for (final String name : usedNames) {
      final String qualified = KNOWN_TYPES.get(name);
      if (qualified != null
          && !importedNames.contains(name)
          && !declaredTypes.contains(name)
          && !wildcardPackages.contains(qualified.substring(0, qualified.lastIndexOf('.')))) {
        regular.add(qualified);
      }
    }

    return replaceImportBlock(source, unit, render(regular, statics));
  }

  /**
   * Simple names used anywhere outside package and import declarations. This
   * over-approximates type usage (a variable called {@code List} would count),
   * which only ever keeps an import, never wrongly removes one.
   */
  private static Set<String> usedSimpleNames(final CompilationUnit unit) {
    final Set<String> names = new HashSet<>();
    unit.accept(new ASTVisitor(true) {
      @Override
      public boolean visit(final ImportDeclaration node) {
        return false;
      }

      @Override
      public boolean visit(final PackageDeclaration node) {
        return false;
      }

      @Override
      public boolean visit(final SimpleName node) {
        names.add(node.getIdentifier());
        return true;
      }
    });
    return names;
  }

  /** Types and type parameters declared in the file, which must never be imported. */
  private static Set<String> declaredTypeNames(final CompilationUnit unit) {
    final Set<String> names = new HashSet<>();
    unit.accept(new ASTVisitor() {
      @Override
      public void postVisit(final ASTNode node) {
        if (node instanceof AbstractTypeDeclaration type) {
          names.add(type.getName().getIdentifier());
        } else if (node instanceof TypeParameter parameter) {
          names.add(parameter.getName().getIdentifier());
        }
      }
    });
    return names;
  }

  /** Renders imports in IntelliJ's default order, or "" when there are none. */
  private static String render(final Set<String> regular, final Set<String> statics) {
    final Comparator<String> byName = Comparator.naturalOrder();
    final List<String> other = regular.stream()
        .filter(name -> !name.startsWith("java.") && !name.startsWith("javax."))
        .sorted(byName).toList();
    final List<String> javaAndJavax = Stream.concat(
            regular.stream().filter(name -> name.startsWith("javax.")).sorted(byName),
            regular.stream().filter(name -> name.startsWith("java.")).sorted(byName))
        .toList();

    final List<String> blocks = new ArrayList<>();
    addBlock(blocks, other, "import ");
    addBlock(blocks, javaAndJavax, "import ");
    addBlock(blocks, List.copyOf(statics), "import static ");
    return String.join("\n", blocks);
  }

  private static void addBlock(final List<String> blocks, final List<String> names,
      final String prefix) {
    if (!names.isEmpty()) {
      blocks.add(names.stream().map(name -> prefix + name + ";\n")
          .collect(Collectors.joining()));
    }
  }

  /**
   * Swaps the existing import block (or the empty spot where it belongs) for
   * the rendered one, keeping exactly one blank line after it.
   */
  private static String replaceImportBlock(final String source, final CompilationUnit unit,
      final String imports) {
    final List<?> existing = unit.imports();
    final int start;
    final int end;
    if (!existing.isEmpty()) {
      final ASTNode first = (ASTNode) existing.get(0);
      final ASTNode last = (ASTNode) existing.get(existing.size() - 1);
      start = first.getStartPosition();
      end = skipBlankLines(source, last.getStartPosition() + last.getLength());
    } else if (imports.isEmpty()) {
      return source;
    } else if (unit.getPackage() != null) {
      final PackageDeclaration pkg = unit.getPackage();
      start = skipBlankLines(source, pkg.getStartPosition() + pkg.getLength());
      end = start;
    } else {
      start = 0;
      end = 0;
    }
    final String replacement = imports.isEmpty() ? "" : imports + "\n";
    return source.substring(0, start) + replacement + source.substring(end);
  }

  private static int skipBlankLines(final String source, final int from) {
    int position = from;
    while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
      position++;
    }
    return position;
  }

  private static Map<String, String> packageTypes(final String packageName,
      final String... simpleNames) {
    return Stream.of(simpleNames)
        .collect(Collectors.toMap(name -> name, name -> packageName + "." + name));
  }
}
