package com.example.onlinejava.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JavaFormatter}: IntelliJ-like output, both source kinds,
 * idempotence, and clear errors for code that doesn't parse.
 */
class JavaFormatterTest {

  private final JavaFormatter formatter = new JavaFormatter();

  @Test
  void formatsWholeClassLikeIntellij() {
    final String messy = "public class Main{public static void main(String[] args){"
        + "int[] a=new int[]{1,2,3};for(int i=0;i<a.length;i++){if(a[i]>1){"
        + "System.out.println(a[i]);}else{continue;}}}}";

    assertThat(formatter.format(messy, SourceKind.CLASS)).isEqualTo("""
        public class Main {
            public static void main(String[] args) {
                int[] a = new int[]{1, 2, 3};
                for (int i = 0; i < a.length; i++) {
                    if (a[i] > 1) {
                        System.out.println(a[i]);
                    } else {
                        continue;
                    }
                }
            }
        }
        """);
  }

  @Test
  void formatsMethodBodyStatementsWithoutWrapperClass() {
    final String body = "int sum=0;\nfor(int n:numbers){sum+=n;}\nreturn sum;";

    assertThat(formatter.format(body, SourceKind.METHOD_BODY)).isEqualTo("""
        int sum = 0;
        for (int n : numbers) {
            sum += n;
        }
        return sum;
        """);
  }

  @Test
  void isIdempotent() {
    final String once = formatter.format(
        "class A{void f(){int x=1;// keep me\n}}", SourceKind.CLASS);

    assertThat(formatter.format(once, SourceKind.CLASS)).isEqualTo(once);
  }

  @Test
  void keepsCommentsAsWritten() {
    final String formatted = formatter.format("""
        class A {
        /**   Javadoc   stays   as   written. */
        void f() { // trailing   comment
        }
        }
        """, SourceKind.CLASS);

    assertThat(formatted)
        .contains("/**   Javadoc   stays   as   written. */")
        .contains("// trailing   comment");
  }

  @Test
  void preservesAtMostOneBlankLine() {
    final String formatted = formatter.format(
        "int a = 1;\n\n\n\nint b = 2;", SourceKind.METHOD_BODY);

    assertThat(formatted).isEqualTo("int a = 1;\n\nint b = 2;\n");
  }

  @Test
  void rejectsCodeWithSyntaxErrorsAndReportsTheLine() {
    assertThatThrownBy(() -> formatter.format("class A {\n  void f() {\n    int x = ;\n  }\n}",
        SourceKind.CLASS))
        .isInstanceOf(UnformattableCodeException.class)
        .hasMessageContaining("Line 3");
    assertThatThrownBy(() ->
        formatter.format("for (int i = 0; i < 3; i++ {", SourceKind.METHOD_BODY))
        .isInstanceOf(UnformattableCodeException.class);
  }

  @Test
  void emptyInputStaysEmpty() {
    assertThat(formatter.format("   \n", SourceKind.METHOD_BODY)).isEmpty();
  }
}
