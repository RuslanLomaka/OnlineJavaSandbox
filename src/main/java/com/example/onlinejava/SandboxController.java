package com.example.onlinejava;

import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.ProblemRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles requests for executing Java source code.
 */
@RestController
public final class SandboxController {

  /**
   * Service responsible for running Java code.
   */
  private final JavaRunnerService javaRunnerService;

  /**
   * Registry containing all available coding problems.
   */
  private final ProblemRegistry problemRegistry;

  /**
   * Creates a sandbox controller.
   *
   * @param runnerService service responsible for executing Java code
   * @param registry registry containing available problems
   */
  public SandboxController(
      final JavaRunnerService runnerService,
      final ProblemRegistry registry
  ) {
    this.javaRunnerService = runnerService;
    this.problemRegistry = registry;
  }

  /**
   * Executes Java source code directly.
   *
   * @param sourceCode complete Java source code
   * @return execution output
   */
  @PostMapping(
      value = "/sandbox/run",
      consumes = MediaType.TEXT_PLAIN_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String run(final @RequestBody String sourceCode) {
    return javaRunnerService.run(sourceCode);
  }

  /**
   * Builds and executes the test source for a registered problem.
   *
   * @param slug problem slug
   * @param solutionCode solution code submitted by the user
   * @return test execution output
   */
  @PostMapping(
      value = "/problems/{slug}/run",
      consumes = MediaType.TEXT_PLAIN_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String runProblem(
      final @PathVariable String slug,
      final @RequestBody String solutionCode
  ) {
    final ProblemDefinition problemDefinition =
        problemRegistry.getProblem(slug);

    if (problemDefinition == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Problem not found"
      );
    }

    final String completeSource =
        problemDefinition.buildTestSource(solutionCode);

    return javaRunnerService.run(completeSource);
  }
}
