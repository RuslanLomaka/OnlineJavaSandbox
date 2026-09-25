package com.example.onlinejava.editor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Shared JDT parsing setup (Java 21 syntax, no classpath or bindings, so it's
 * fast and needs nothing from the user's project).
 */
final class JavaSyntax {

  private JavaSyntax() {
  }

  /**
   * Returns JDT compiler options for Java 21 source.
   *
   * @return mutable options map
   */
  static Map<String, String> compilerOptions() {
    final Map<String, String> options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
    return options;
  }

  /**
   * Parses source and throws if it has syntax errors.
   *
   * @param source code to parse
   * @param kind whole class or method-body statements
   * @return the parsed compilation unit (for {@link SourceKind#METHOD_BODY},
   *     the unit JDT creates around the statements)
   * @throws UnformattableCodeException describing the first syntax error
   */
  static CompilationUnit parseOrThrow(final String source, final SourceKind kind) {
    final ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
    parser.setKind(kind == SourceKind.CLASS ? ASTParser.K_COMPILATION_UNIT
        : ASTParser.K_STATEMENTS);
    parser.setCompilerOptions(compilerOptions());
    parser.setSource(source.toCharArray());
    final ASTNode node = parser.createAST(null);
    final CompilationUnit unit = (CompilationUnit) node.getRoot();

    Arrays.stream(unit.getProblems())
        .filter(IProblem::isError)
        .min(Comparator.comparingInt(IProblem::getSourceStart))
        .ifPresent(problem -> {
          throw new UnformattableCodeException(
              "Line " + lineOf(source, problem.getSourceStart()) + ": " + problem.getMessage());
        });
    return unit;
  }

  private static int lineOf(final String source, final int offset) {
    final int end = Math.min(Math.max(offset, 0), source.length());
    int line = 1;
    for (int i = 0; i < end; i++) {
      if (source.charAt(i) == '\n') {
        line++;
      }
    }
    return line;
  }
}
