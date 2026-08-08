package com.example.onlinejava;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JavaRunnerService {

    private static final String DOCKER_IMAGE =
            "eclipse-temurin:21-jdk";

    private static final int EXECUTION_TIMEOUT_SECONDS = 100;

    private final Path sandboxRoot;
    private final boolean noNewPrivileges;

    public JavaRunnerService(
            @Value("${sandbox.root}") String sandboxRoot,
            @Value("${sandbox.no-new-privileges:true}")
            boolean noNewPrivileges
    ) {
        this.sandboxRoot = Path.of(sandboxRoot);
        this.noNewPrivileges = noNewPrivileges;
    }

    public String run(String sourceCode) {
        Path temporaryDirectory = null;
        String containerName = null;

        try {
            /*
             * This directory is mounted into the Spring container
             * from the Raspberry Pi host using compose.yaml.
             *
             * Pi host:
             * /tmp/online-java-runs
             *
             * Spring container:
             * /tmp/online-java-runs
             *
             * The identical path is important because the Docker
             * daemon runs on the Pi host and must be able to find
             * the temporary directory used as a bind mount.
             */
            Files.createDirectories(sandboxRoot);

            /*
             * Create a unique directory for this execution.
             *
             * Example:
             * /tmp/online-java-runs/java-sandbox-123456/
             */
            temporaryDirectory =
                    Files.createTempDirectory(
                            sandboxRoot,
                            "java-sandbox-"
                    );

            /*
             * Create:
             * /tmp/online-java-runs/java-sandbox-123456/Main.java
             */
            Path sourceFile =
                    temporaryDirectory.resolve("Main.java");

            /*
             * Write the source code received from the browser
             * into a real Java source file.
             */
            Files.writeString(
                    sourceFile,
                    sourceCode,
                    StandardCharsets.UTF_8
            );

            makeSourceReadableByRunner(
                    temporaryDirectory,
                    sourceFile
            );

            /*
             * Docker output is redirected into this host file.
             * This prevents stdout or stderr from filling the
             * process pipe and blocking the Java application.
             */
            Path outputFile =
                    temporaryDirectory.resolve("docker-output.txt");

            /*
             * Every execution gets a unique container name.
             */
            containerName =
                    "java-sandbox-" + UUID.randomUUID();

            /*
             * This path exists both inside the Spring container
             * and on the Raspberry Pi host.
             */
            String hostDirectory =
                    temporaryDirectory
                            .toAbsolutePath()
                            .toString();

            /*
             * Start a new disposable Docker container.
             */
            List<String> dockerCommand = new ArrayList<>(List.of(
                    "docker",
                    "run",

                    "--name",
                    containerName,

                    "--rm",

                    "--network",
                    "none",

                    "--cpus",
                    "2",

                    "--pids-limit",
                    "32",

                    "--cap-drop",
                    "ALL"
            ));

            if (noNewPrivileges) {
                dockerCommand.add("--security-opt");
                dockerCommand.add("no-new-privileges");
            }

            dockerCommand.addAll(List.of(

                    "--read-only",

                    "--mount",
                    "type=bind,source="
                            + hostDirectory
                            + ",target=/source,readonly",

                    "--tmpfs",
                    "/work:rw,nosuid,size=64m",

                    "--entrypoint",
                    "sh",

                    DOCKER_IMAGE,

                    "-c",

                    /*
                     * Commands executed inside the runner:
                     *
                     * 1. Copy Main.java from the read-only mount.
                     * 2. Enter the writable RAM-backed directory.
                     * 3. Compile Main.java.
                     * 4. Run Main.
                     */
                    "cp /source/Main.java /work/Main.java"
                            + " && cd /work"
                            + " && javac Main.java"
                            + " && java Main"
            ));

            Process dockerProcess = new ProcessBuilder(dockerCommand)
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile())
                    .start();

            /*
             * Wait for compilation and execution.
             */
            boolean finished = dockerProcess.waitFor(
                    EXECUTION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            if (!finished) {
                /*
                 * Kill the Docker CLI process.
                 * The named container is also removed in finally.
                 */
                dockerProcess.destroyForcibly();
                dockerProcess.waitFor(
                        2,
                        TimeUnit.SECONDS
                );

                return "Execution timed out.";
            }

            /*
             * Read compiler and program output.
             */
            String output = Files.exists(outputFile)
                    ? Files.readString(
                    outputFile,
                    StandardCharsets.UTF_8
            )
                    : "";

            /*
             * Simplify compiler paths.
             *
             * Example:
             * /work/Main.java:5: error
             *
             * becomes:
             * Main.java:5: error
             */
            output = output.replaceAll(
                    "(?m)^.*[\\\\/]Main\\.java",
                    "Main.java"
            );

            if (output.isBlank()) {
                output =
                        "(Program finished without output)\n";
            }

            return output
                    + "\nProcess finished with exit code "
                    + dockerProcess.exitValue();

        } catch (IOException exception) {
            return "Docker process error:\n"
                    + exception.getMessage();

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "Execution was interrupted.";

        } finally {
            /*
             * Remove a container left behind after timeout
             * or another unexpected error.
             */
            removeContainer(containerName);

            /*
             * Delete Main.java and docker-output.txt.
             */
            deleteDirectory(temporaryDirectory);
        }
    }

    private void removeContainer(String containerName) {
        if (containerName == null) {
            return;
        }

        try {
            Process cleanup = new ProcessBuilder(
                    "docker",
                    "rm",
                    "-f",
                    containerName
            )
                    .redirectErrorStream(true)
                    .start();

            cleanup.waitFor(
                    3,
                    TimeUnit.SECONDS
            );

        } catch (IOException exception) {
            System.err.println(
                    "Could not remove Docker container: "
                            + exception.getMessage()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void makeSourceReadableByRunner(
            Path temporaryDirectory,
            Path sourceFile
    ) throws IOException {
        if (!Files.getFileStore(sourceFile)
                .supportsFileAttributeView("posix")) {
            return;
        }

        Files.setPosixFilePermissions(
                temporaryDirectory,
                PosixFilePermissions.fromString("rwxr-xr-x")
        );

        Files.setPosixFilePermissions(
                sourceFile,
                PosixFilePermissions.fromString("rw-r--r--")
        );
    }

    private void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);

                        } catch (IOException exception) {
                            System.err.println(
                                    "Could not delete " + path
                            );
                        }
                    });

        } catch (IOException exception) {
            System.err.println(
                    "Cleanup failed: "
                            + exception.getMessage()
            );
        }
    }
}
