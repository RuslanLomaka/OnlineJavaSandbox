package com.example.onlinejava.editor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ImportOrganizer}, the server side of Ctrl+Alt+O.
 */
class ImportOrganizerTest {

  private final ImportOrganizer organizer = new ImportOrganizer();

  @Test
  void removesUnusedAndAddsMissingImportsInIntellijOrder() {
    final String source = """
        import java.io.File;
        import java.util.List;

        public class Main {
            public static void main(String[] args) {
                List<Integer> list = new ArrayList<>();
                Map<String, Integer> counts = new HashMap<>();
                System.out.println(list + " " + counts);
            }
        }
        """;

    assertThat(organizer.organize(source)).startsWith("""
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;

        public class Main {""");
  }

  @Test
  void neverImportsJavaLangOrLocallyDeclaredTypes() {
    final String source = """
        public class Main {
            static class Node { Node next; }
            public static void main(String[] args) {
                String s = String.valueOf(Math.max(1, 2));
                StringBuilder sb = new StringBuilder(s);
                Node node = new Node();
            }
        }
        """;

    assertThat(organizer.organize(source)).isEqualTo(source);
  }

  @Test
  void addsImportsForStaticCallsAndNestedTypes() {
    final String source = """
        public class Main {
            void f(int[] numbers, Map<String, Integer> map) {
                Arrays.sort(numbers);
                for (Map.Entry<String, Integer> e : map.entrySet()) {
                    System.out.println(e);
                }
                var joined = Stream.of("a").collect(Collectors.joining());
            }
        }
        """;

    assertThat(organizer.organize(source)).startsWith("""
        import java.util.Arrays;
        import java.util.Map;
        import java.util.stream.Collectors;
        import java.util.stream.Stream;

        public class Main {""");
  }

  @Test
  void groupsOtherThenJavaxThenJavaThenStatic() {
    final String source = """
        import static java.lang.Math.max;
        import java.util.List;
        import javax.swing.JPanel;
        import org.example.Thing;

        class A {
            List<Thing> things;
            JPanel panel;
            int m = max(1, 2);
        }
        """;

    assertThat(organizer.organize(source)).startsWith("""
        import org.example.Thing;

        import javax.swing.JPanel;
        import java.util.List;

        import static java.lang.Math.max;

        class A {""");
  }

  @Test
  void removesUnusedStaticImportsButKeepsWildcards() {
    final String source = """
        import static java.lang.Math.abs;
        import java.util.*;

        class A {
            List<String> names;
        }
        """;

    assertThat(organizer.organize(source)).startsWith("""
        import java.util.*;

        class A {""");
  }

  @Test
  void placesNewImportsAfterPackageDeclaration() {
    final String source = """
        package demo;

        class A {
            List<String> names;
        }
        """;

    assertThat(organizer.organize(source)).isEqualTo("""
        package demo;

        import java.util.List;

        class A {
            List<String> names;
        }
        """);
  }

  @Test
  void leavesSourceWithoutImportChangesUntouched() {
    final String source = "public class Main {\n    int x = 1;\n}\n";

    assertThat(organizer.organize(source)).isEqualTo(source);
  }
}
