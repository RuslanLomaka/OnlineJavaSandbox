package com.example.onlinejava;

import java.io.File;
import java.util.Locale;

/**
 * Resolves the {@code docker} executable's absolute path once at class
 * load time by scanning the process's {@code PATH} environment
 * variable, so later {@link ProcessBuilder} invocations use a fixed
 * path rather than re-resolving a bare command name through
 * {@code PATH} on every call.
 */
final class DockerExecutable {

  private static final String RESOLVED_PATH = resolve();

  private DockerExecutable() {
  }

  /**
   * Returns the absolute path to the {@code docker} executable, or the
   * bare command name {@code "docker"} as a fallback if it could not
   * be located on {@code PATH}.
   *
   * @return the path to use when invoking Docker via ProcessBuilder
   */
  static String path() {
    return RESOLVED_PATH;
  }

  private static String resolve() {
    String pathEnvironmentVariable = System.getenv("PATH");

    if (pathEnvironmentVariable == null) {
      return "docker";
    }

    boolean isWindows = System.getProperty("os.name", "")
        .toLowerCase(Locale.ROOT)
        .contains("win");
    String executableName = isWindows ? "docker.exe" : "docker";

    for (String directory : pathEnvironmentVariable.split(File.pathSeparator)) {
      File candidate = new File(directory, executableName);

      if (candidate.isFile()) {
        return candidate.getAbsolutePath();
      }
    }

    return "docker";
  }
}
