package com.example.onlinejava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically reaps Docker containers and temporary directories left
 * behind when a sandbox execution's JVM process dies before the normal
 * cleanup in {@link JavaRunnerService#run} can run.
 *
 * <p>Under normal operation this finds nothing: every execution cleans
 * up its own container and temp directory in a {@code finally} block.
 * This is only a safety net for the case where the host JVM itself is
 * killed mid-execution.
 */
@Component
public class SandboxReaperService {

  /**
   * Prefix shared by both sandbox container names and sandbox temp
   * directory names, used to identify which containers/directories are
   * safe for this reaper to remove.
   */
  private static final String SANDBOX_NAME_PREFIX = "java-sandbox-";

  /**
   * How old, in seconds, a sandbox container or temp directory must be
   * before it is considered orphaned. Comfortably larger than
   * {@code JavaRunnerService}'s execution timeout so an in-flight
   * execution is never mistaken for an orphan.
   */
  private static final long ORPHAN_AGE_THRESHOLD_SECONDS = 180;

  /**
   * How often, in milliseconds, the reaper sweep runs.
   */
  private static final long REAPER_INTERVAL_MILLIS = 120_000;

  private final Path sandboxRoot;

  /**
   * Constructs a new SandboxReaperService.
   *
   * @param sandboxRootPath the root directory path where sandbox
   *     execution temp directories are created
   */
  public SandboxReaperService(@Value("${sandbox.root}") final String sandboxRootPath) {
    this.sandboxRoot = Path.of(sandboxRootPath);
  }

  /**
   * Finds and removes sandbox Docker containers and temp directories
   * older than {@value #ORPHAN_AGE_THRESHOLD_SECONDS} seconds.
   */
  @Scheduled(fixedDelay = REAPER_INTERVAL_MILLIS)
  public void reapOrphans() {
    reapOrphanedContainers();
    reapOrphanedDirectories();
  }

  private void reapOrphanedContainers() {
    for (String containerId : listSandboxContainerIds()) {
      Instant startedAt = containerStartTime(containerId);

      if (startedAt != null && isOlderThanThreshold(startedAt)) {
        removeContainer(containerId);
      }
    }
  }

  private List<String> listSandboxContainerIds() {
    List<String> containerIds = new ArrayList<>();

    try {
      Process process = new ProcessBuilder(
          DockerExecutable.path(), "ps", "--filter", "name=" + SANDBOX_NAME_PREFIX, "--format",
          "{{.ID}}")
          .redirectErrorStream(true)
          .start();

      List<String> lines = readLines(process);
      boolean finished = process.waitFor(5, TimeUnit.SECONDS);

      if (finished && process.exitValue() == 0) {
        containerIds.addAll(lines);
      } else {
        System.err.println("docker ps did not exit cleanly; skipping this reaper cycle");
      }

    } catch (IOException exception) {
      System.err.println("Could not list sandbox containers: " + exception.getMessage());

    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }

    return containerIds;
  }

  private Instant containerStartTime(final String containerId) {
    try {
      Process process = new ProcessBuilder(
          DockerExecutable.path(), "inspect", "-f", "{{.State.StartedAt}}", containerId)
          .redirectErrorStream(true)
          .start();

      List<String> lines = readLines(process);
      boolean finished = process.waitFor(5, TimeUnit.SECONDS);

      if (!finished || process.exitValue() != 0 || lines.isEmpty()) {
        return null;
      }

      return Instant.parse(lines.get(0));

    } catch (IOException | DateTimeParseException exception) {
      System.err.println("Could not read start time for container "
          + containerId + ": " + exception.getMessage());
      return null;

    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private void removeContainer(final String containerId) {
    try {
      Process process = new ProcessBuilder(DockerExecutable.path(), "rm", "-f", containerId)
          .redirectErrorStream(true)
          .start();

      process.waitFor(5, TimeUnit.SECONDS);

    } catch (IOException exception) {
      System.err.println("Could not remove orphaned container "
          + containerId + ": " + exception.getMessage());

    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  private void reapOrphanedDirectories() {
    if (!Files.exists(sandboxRoot)) {
      return;
    }

    try (var entries = Files.list(sandboxRoot)) {
      entries
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith(SANDBOX_NAME_PREFIX))
          .filter(this::isOlderThanThreshold)
          .forEach(this::deleteDirectory);

    } catch (IOException exception) {
      System.err.println("Could not scan sandbox root for orphans: " + exception.getMessage());
    }
  }

  private boolean isOlderThanThreshold(final Path directory) {
    try {
      Instant lastModified = Files.getLastModifiedTime(directory).toInstant();
      return isOlderThanThreshold(lastModified);

    } catch (IOException exception) {
      System.err.println("Could not read last-modified time for "
          + directory + ": " + exception.getMessage());
      return false;
    }
  }

  private boolean isOlderThanThreshold(final Instant instant) {
    return Duration.between(instant, Instant.now()).getSeconds() > ORPHAN_AGE_THRESHOLD_SECONDS;
  }

  private void deleteDirectory(final Path directory) {
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
      System.err.println("Could not delete orphaned directory "
          + directory + ": " + exception.getMessage());
    }
  }

  private List<String> readLines(final Process process) throws IOException {
    List<String> lines = new ArrayList<>();

    try (var reader = new BufferedReader(
        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

      String line = reader.readLine();
      while (line != null) {
        if (!line.isBlank()) {
          lines.add(line.trim());
        }
        line = reader.readLine();
      }
    }

    return lines;
  }
}
