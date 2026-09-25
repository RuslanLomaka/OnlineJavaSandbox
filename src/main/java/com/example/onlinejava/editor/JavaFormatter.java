package com.example.onlinejava.editor;

import static org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER;
import static org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER;
import static org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER;

import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.springframework.stereotype.Component;

/**
 * Formats Java source like IntelliJ IDEA's default code style, using the
 * Eclipse JDT formatter (pure Java, so it runs on a JRE).
 *
 * <p>Style: 4-space indentation, 120 columns, continuation indent 8, braces at
 * end of line, {@code new int[]{1, 2}} array initializers, comments kept
 * exactly as written, at most one consecutive blank line.
 */
@Component
public class JavaFormatter {

  private static final String LINE_SEPARATOR = "\n";

  /**
   * Formats source code.
   *
   * @param source code to format
   * @param kind whole class or method-body statements
   * @return formatted code ending with a newline, or empty for blank input
   * @throws UnformattableCodeException if the code has syntax errors
   */
  public String format(final String source, final SourceKind kind) {
    if (source.isBlank()) {
      return "";
    }
    // JDT silently returns an empty edit for broken code, so check first and
    // give the user a real error message instead of "nothing happened".
    JavaSyntax.parseOrThrow(source, kind);

    final CodeFormatter formatter = ToolFactory.createCodeFormatter(options());
    final int jdtKind = (kind == SourceKind.CLASS
        ? CodeFormatter.K_COMPILATION_UNIT : CodeFormatter.K_STATEMENTS)
        | CodeFormatter.F_INCLUDE_COMMENTS;
    final TextEdit edit = formatter.format(jdtKind, source, 0, source.length(), 0,
        LINE_SEPARATOR);
    if (edit == null) {
      throw new UnformattableCodeException("The code could not be formatted");
    }
    final Document document = new Document(source);
    try {
      edit.apply(document);
    } catch (BadLocationException e) {
      throw new IllegalStateException("JDT produced an invalid edit", e);
    }
    return document.get().strip() + LINE_SEPARATOR;
  }

  private static Map<String, String> options() {
    final Map<String, String> options = DefaultCodeFormatterConstants.getEclipseDefaultSettings();
    options.putAll(JavaSyntax.compilerOptions());
    options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
    options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
    options.put(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "4");
    options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "120");
    options.put(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, "2");
    options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
    // IntelliJ keeps the user's own line breaks inside long expressions.
    options.put(DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES,
        DefaultCodeFormatterConstants.FALSE);
    // Comments are the user's words: never reflow them.
    options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT,
        DefaultCodeFormatterConstants.FALSE);
    options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT,
        DefaultCodeFormatterConstants.FALSE);
    options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT,
        DefaultCodeFormatterConstants.FALSE);
    // IntelliJ writes new int[]{1, 2}, Eclipse's default is new int[] { 1, 2 }.
    options.put(FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER,
        JavaCore.DO_NOT_INSERT);
    options.put(FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER,
        JavaCore.DO_NOT_INSERT);
    options.put(FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER,
        JavaCore.DO_NOT_INSERT);
    return options;
  }
}
