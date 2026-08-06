package com.example.onlinejava;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JavaRunnerService {

    private static final String DOCKER_IMAGE =
            "eclipse-temurin:21-jdk";

    private static final int EXECUTION_TIMEOUT_SECONDS = 8;

    public String run(String sourceCode) {
        Path temporaryDirectory = null;
        String containerName = null;

        try {
            /*
             * 1. Create a temporary folder on Windows.
             *
             * Example:
             * C:\Users\...\Temp\java-sandbox-123456\
             */
            temporaryDirectory =
                    Files.createTempDirectory("java-sandbox-");

            /*
             * 2. Create the path:
             *
             * C:\Users\...\Temp\java-sandbox-123456\Main.java
             */
            Path sourceFile =
                    temporaryDirectory.resolve("Main.java");

            /*
             * 3. Turn the text received from the webpage
             * into a real Main.java file.
             */
            Files.writeString(
                    sourceFile,
                    sourceCode,
                    StandardCharsets.UTF_8
            );

            /*
             * Docker's output will be written here.
             * This avoids the process blocking if it prints
             * more output than the process pipe can hold.
             */
            Path outputFile =
                    temporaryDirectory.resolve("docker-output.txt");

            /*
             * Every execution gets a unique container name.
             * This lets us forcibly remove the container
             * if the execution reaches the timeout.
             */
            containerName =
                    "java-sandbox-" + UUID.randomUUID();

            String hostDirectory =
                    temporaryDirectory
                            .toAbsolutePath()
                            .toString();

            /*
             * 4. Start a completely new Docker container.
             *
             * The Windows temporary directory becomes visible
             * inside the container at /source.
             *
             * Host:
             * C:\...\java-sandbox-123\Main.java
             *
             * Container:
             * /source/Main.java
             */
            Process dockerProcess = new ProcessBuilder(
                    "docker",
                    "run",

                    "--name",
                    containerName,

                    "--rm",

                    "--network",
                    "none",

                    "--memory",
                    "256m",

                    "--memory-swap",
                    "256m",

                    "--cpus",
                    "0.5",

                    "--pids-limit",
                    "32",

                    "--cap-drop",
                    "ALL",

                    "--security-opt",
                    "no-new-privileges",

                    "--read-only",

                    "--mount",
                    "type=bind,source="
                            + hostDirectory
                            + ",target=/source,readonly",

                    "--tmpfs",
                    "/work:rw,nosuid,size=64m",

                    DOCKER_IMAGE,

                    "sh",
                    "-c",

                    /*
                     * These commands execute inside the container:
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
            )
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile())
                    .start();

            /*
             * 5. Wait for compilation and execution.
             */
            boolean finished = dockerProcess.waitFor(
                    EXECUTION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            if (!finished) {
                /*
                 * Killing the Docker CLI process alone may not be
                 * enough, so we also remove the named container
                 * in the finally block.
                 */
                dockerProcess.destroyForcibly();
                dockerProcess.waitFor(2, TimeUnit.SECONDS);

                return "Execution timed out.";
            }

            /*
             * 6. Read javac/java output produced by Docker.
             */
            String output = Files.exists(outputFile)
                    ? Files.readString(
                    outputFile,
                    StandardCharsets.UTF_8
            )
                    : "";

            /*
             * Change paths such as:
             *
             * /work/Main.java:5: error...
             *
             * into:
             *
             * Main.java:5: error...
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
             * If a timeout or error left the container running,
             * forcibly remove it.
             */
            removeContainer(containerName);

            /*
             * Delete Main.java and docker-output.txt
             * from the Windows temporary folder.
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

            cleanup.waitFor(3, TimeUnit.SECONDS);

        } catch (IOException exception) {
            System.err.println(
                    "Could not remove Docker container: "
                            + exception.getMessage()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
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