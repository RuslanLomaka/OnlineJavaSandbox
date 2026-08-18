package com.example.onlinejava;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Compiles and executes user-submitted Java source code inside an
 * isolated, resource-limited Docker container.
 */
@Service
public class JavaRunnerService {

  /**
   * The Docker image used for running Java code in a sandboxed
   * environment, pinned by digest rather than the {@code 21-jdk} tag so
   * a tag update upstream can't silently change what runs in
   * production without this being updated deliberately.
   */
  private static final String DOCKER_IMAGE =
      "eclipse-temurin@sha256:efd34b940f2d5a621605c8531c2afb7759c936b6c2ef637a69aa3bf3e1e789d1";

  /**
   * The maximum number of seconds to wait for Docker container
   * execution to complete. If the execution exceeds this timeout,
   * the container will be forcibly terminated.
   */
  private static final int EXECUTION_TIMEOUT_SECONDS = 100;

  /**
   * The maximum number of bytes of combined stdout/stderr output that
   * will be captured from a single execution. Output beyond this
   * limit causes the container to be killed early.
   */
  private static final long MAX_OUTPUT_BYTES = 1_048_576;

  /**
   * How often, in milliseconds, the running container's output file
   * size is polled to enforce {@value #MAX_OUTPUT_BYTES}.
   */
  private static final long OUTPUT_WATCH_INTERVAL_MILLIS = 200;

  /**
   * The maximum number of Docker sandbox executions allowed to run at
   * the same time. Chosen to match how many {@code --cpus 2}
   * containers the host can run without CPU contention. Requests
   * beyond this limit wait their turn rather than being rejected.
   */
  private static final int MAX_CONCURRENT_EXECUTIONS = 2;

  /**
   * Bounds how many executions run concurrently. Acquired before a
   * container starts and released once it finishes.
   */
  private final Semaphore executionSlots = new Semaphore(MAX_CONCURRENT_EXECUTIONS);

  /**
   * The root directory path where temporary sandbox execution
   * environments are created for running user-submitted Java code.
   */
  private final Path sandboxRoot;

  /**
   * Whether to enable the no-new-privileges security option for
   * Docker containers.
   * When true, prevents processes inside the container from gaining
   * additional privileges through mechanisms like setuid binaries.
   */
  private final boolean noNewPrivileges;

  /**
   * Constructs a new JavaRunnerService with the specified sandbox
   * configuration.
   *
   * @param sandboxRootPath the root directory path where temporary
   *     sandbox execution environments will be created
   * @param noNewPrivilegesFlag whether to enable the no-new-privileges
   *     security option for Docker containers
   */
  public JavaRunnerService(@Value("${sandbox.root}") final String sandboxRootPath,
                           @Value("${sandbox.no-new-privileges:true}")
                           final boolean noNewPrivilegesFlag) {
    this.sandboxRoot = Path.of(sandboxRootPath);
    this.noNewPrivileges = noNewPrivilegesFlag;
  }

  /**
   * Executes user-submitted Java source code in a sandboxed Docker
   * container and returns the compilation and execution output.
   *
   * <p>This method performs the following steps:
   * <ol>
   *   <li>Creates a temporary directory for the execution
   *       environment</li>
   *   <li>Writes the source code to a Main.java file</li>
   *   <li>Sets appropriate file permissions for the source file</li>
   *   <li>Waits for a free execution slot if
   *       {@value #MAX_CONCURRENT_EXECUTIONS} executions are already
   *       running</li>
   *   <li>Starts a sandboxed Docker container with resource limits
   *       and security constraints</li>
   *   <li>Compiles and executes the Java code inside the
   *       container</li>
   *   <li>Waits for execution to complete or times out after
   *       {@value #EXECUTION_TIMEOUT_SECONDS} seconds</li>
   *   <li>Returns the combined compilation and execution output</li>
   * </ol>
   *
   * <p>The Docker container is configured with the following
   * security measures:
   * <ul>
   *   <li>No network access</li>
   *   <li>Limited CPU, memory, and process count</li>
   *   <li>Limited open file descriptors and per-file size (ulimits)</li>
   *   <li>Captured output capped at {@value #MAX_OUTPUT_BYTES} bytes</li>
   *   <li>All capabilities dropped</li>
   *   <li>Read-only filesystem with limited writable tmpfs</li>
   *   <li>Optional no-new-privileges security option</li>
   * </ul>
   *
   * <p>The temporary directory and Docker container are cleaned up
   * automatically after execution, regardless of success or failure.
   *
   * @param sourceCode the Java source code to compile and execute,
   *     which should contain a public class named Main
   * @return the output from compilation and execution, including any
   *     error messages, or a timeout/error message if execution
   *     fails
   */
  public String run(final String sourceCode) {
    Path temporaryDirectory = null;
    String containerName = null;

    try {
      Files.createDirectories(sandboxRoot);

      temporaryDirectory = Files.createTempDirectory(sandboxRoot, "java-sandbox-");

      Path sourceFile = writeSourceFile(temporaryDirectory, sourceCode);
      makeSourceReadableByRunner(temporaryDirectory, sourceFile);

      containerName = "java-sandbox-" + UUID.randomUUID();
      Path outputFile = temporaryDirectory.resolve("docker-output.txt");

      List<String> dockerCommand = buildDockerCommand(temporaryDirectory, containerName);

      executionSlots.acquire();
      try {
        return executeDockerCommand(dockerCommand, outputFile);
      } finally {
        executionSlots.release();
      }

    } catch (IOException exception) {
      return "Docker process error:\n" + exception.getMessage();

    } catch (InterruptedException exception) {
      Thread
          .currentThread()
          .interrupt();
      return "Execution was interrupted.";

    } finally {
      removeContainer(containerName);
      deleteDirectory(temporaryDirectory);
    }
  }

  private Path writeSourceFile(final Path temporaryDirectory, final String sourceCode)
      throws IOException {
    Path sourceFile = temporaryDirectory.resolve("Main.java");
    Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
    return sourceFile;
  }

  private List<String> buildDockerCommand(final Path temporaryDirectory,
                                          final String containerName) {
    String hostDirectory = temporaryDirectory
        .toAbsolutePath()
        .toString();

    List<String> dockerCommand = new ArrayList<>(
        List.of("docker", "run", "--name", containerName, "--rm", "--network", "none", "--cpus",
            "2", "--memory", "256m", "--memory-swap", "256m", "--pids-limit", "32", "--cap-drop",
            "ALL", "--ulimit", "nofile=1024:1024", "--ulimit", "fsize=67108864:67108864"));

    if (noNewPrivileges) {
      dockerCommand.add("--security-opt");
      dockerCommand.add("no-new-privileges");
    }

    dockerCommand.addAll(List.of("--read-only", "--mount",
        "type=bind,source=" + hostDirectory + ",target=/source,readonly", "--tmpfs",
        "/work:rw,nosuid,size=64m", "--entrypoint", "sh", DOCKER_IMAGE, "-c",
        "cp /source/Main.java /work/Main.java" + " && cd /work" + " && javac Main.java"
            + " && java Main"));

    return dockerCommand;
  }

  private String executeDockerCommand(final List<String> dockerCommand, final Path outputFile)
      throws IOException, InterruptedException {
    Process dockerProcess = new ProcessBuilder(dockerCommand)
        .redirectErrorStream(true)
        .redirectOutput(outputFile.toFile())
        .start();

    Thread outputWatcher = startOutputSizeWatcher(dockerProcess, outputFile);

    boolean finished = dockerProcess.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    outputWatcher.interrupt();

    if (!finished) {
      dockerProcess.destroyForcibly();
      dockerProcess.waitFor(2, TimeUnit.SECONDS);
      return "Execution timed out.";
    }

    return formatOutput(outputFile, dockerProcess.exitValue());
  }

  private Thread startOutputSizeWatcher(final Process process, final Path outputFile) {
    Thread watcher = new Thread(() -> {
      try {
        while (process.isAlive()) {
          if (Files.exists(outputFile) && Files.size(outputFile) > MAX_OUTPUT_BYTES) {
            process.destroyForcibly();
            return;
          }
          Thread.sleep(OUTPUT_WATCH_INTERVAL_MILLIS);
        }
      } catch (IOException exception) {
        System.err.println("Could not check output size: " + exception.getMessage());
      } catch (InterruptedException exception) {
        Thread
            .currentThread()
            .interrupt();
      }
    });
    watcher.setDaemon(true);
    watcher.start();
    return watcher;
  }

  private String formatOutput(final Path outputFile, final int exitCode) throws IOException {
    boolean truncated = Files.exists(outputFile) && Files.size(outputFile) > MAX_OUTPUT_BYTES;

    String output = readOutput(outputFile);

    output = output.replaceAll("(?m)^.*[\\\\/]Main\\.java", "Main.java");

    if (output.isBlank()) {
      output = "(Program finished without output)\n";
    }

    if (truncated) {
      output += "\n(output truncated - exceeded " + (MAX_OUTPUT_BYTES / 1024) + "KB limit)";
    }

    return output + "\nProcess finished with exit code " + exitCode;
  }

  private String readOutput(final Path outputFile) throws IOException {
    if (!Files.exists(outputFile)) {
      return "";
    }

    try (var inputStream = Files.newInputStream(outputFile)) {
      byte[] bytes = inputStream.readNBytes((int) MAX_OUTPUT_BYTES);
      return new String(bytes, StandardCharsets.UTF_8);
    }
  }

  private void removeContainer(String containerName) {
    if (containerName == null) {
      return;
    }

    try {
      Process cleanup = new ProcessBuilder("docker", "rm", "-f", containerName)
          .redirectErrorStream(true)
          .start();

      cleanup.waitFor(3, TimeUnit.SECONDS);

    } catch (IOException exception) {
      System.err.println("Could not remove Docker container: " + exception.getMessage());

    } catch (InterruptedException exception) {
      Thread
          .currentThread()
          .interrupt();
    }
  }

  private void makeSourceReadableByRunner(Path temporaryDirectory, Path sourceFile)
      throws IOException {
    if (!Files
        .getFileStore(sourceFile)
        .supportsFileAttributeView("posix")) {
      return;
    }

    Files.setPosixFilePermissions(temporaryDirectory, PosixFilePermissions.fromString("rwxr-xr-x"));

    Files.setPosixFilePermissions(sourceFile, PosixFilePermissions.fromString("rw-r--r--"));
  }

  private void deleteDirectory(Path directory) {
    if (directory == null || !Files.exists(directory)) {
      return;
    }

    try (var paths = Files.walk(directory)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.deleteIfExists(path);

            } catch (IOException exception) {
              System.err.println("Could not delete " + path);
            }
          });

    } catch (IOException exception) {
      System.err.println("Cleanup failed: " + exception.getMessage());
    }
  }
}
