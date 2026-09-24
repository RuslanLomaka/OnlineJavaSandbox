package com.example.onlinejava.problem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Compiles and runs every problem's hidden test harness, exactly as the
 * sandbox would, with a known-correct and a known-wrong solution.
 *
 * <p>This proves each harness actually accepts right answers and rejects wrong
 * ones. Reference solutions live in {@code src/test/resources/solutions/}
 * ({@code {slug}.java} and {@code {slug}.wrong.java}); adding a problem without
 * them fails this test on purpose. Each harness runs in a separate JVM because
 * harnesses call {@code System.exit(1)} on failure.
 */
class HarnessVerificationTest {

  private static final String SUCCESS_MARKER = "All tests passed!";

  static Stream<ProblemDefinition> definitions() {
    return new ProblemRegistry().getAllProblems().stream();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("definitions")
  void referenceSolutionPassesAllHiddenTests(
      final ProblemDefinition definition,
      @TempDir final Path workDir
  ) throws Exception {
    final Result result = compileAndRun(definition, "", workDir);

    assertThat(result.output()).contains(SUCCESS_MARKER);
    assertThat(result.exitCode()).as(result.output()).isZero();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("definitions")
  void wrongSolutionIsRejected(
      final ProblemDefinition definition,
      @TempDir final Path workDir
  ) throws Exception {
    final Result result = compileAndRun(definition, ".wrong", workDir);

    assertThat(result.output()).doesNotContain(SUCCESS_MARKER);
    assertThat(result.exitCode()).as(result.output()).isNotZero();
  }

  private record Result(int exitCode, String output) {
  }

  private static Result compileAndRun(
      final ProblemDefinition definition,
      final String variant,
      final Path workDir
  ) throws IOException, InterruptedException {
    final String slug = definition.getProblem().getSlug();
    final String solution = readSolution(slug + variant + ".java");
    final Path source = workDir.resolve("Main.java");
    Files.writeString(source, definition.buildTestSource(solution), StandardCharsets.UTF_8);

    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    final ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
    final int compiled = compiler.run(null, compilerOutput, compilerOutput,
        "-encoding", "UTF-8", "-d", workDir.toString(), source.toString());
    assertThat(compiled)
        .as("Harness for %s%s does not compile:%n%s", slug, variant, compilerOutput)
        .isZero();

    final Path java = Path.of(System.getProperty("java.home"), "bin", "java");
    final Process process = new ProcessBuilder(
        java.toString(), "-Dfile.encoding=UTF-8", "-cp", workDir.toString(), "Main")
        .redirectErrorStream(true)
        .start();
    final byte[] output;
    try (InputStream in = process.getInputStream()) {
      output = in.readAllBytes();
    }
    assertThat(process.waitFor(60, TimeUnit.SECONDS)).as("harness timed out").isTrue();
    return new Result(process.exitValue(), new String(output, StandardCharsets.UTF_8));
  }

  private static String readSolution(final String fileName) throws IOException {
    try (InputStream in = HarnessVerificationTest.class
        .getResourceAsStream("/solutions/" + fileName)) {
      assertThat(in).as("Missing reference solution src/test/resources/solutions/%s", fileName)
          .isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
