package com.example.onlinejava.sandbox;

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
 * Handles HTTP requests for executing Java source code.
 *
 * <p>This controller does not run any code itself. Both endpoints hand
 * the source off to {@link SandboxExecutionGateway}, which sends it to
 * RabbitMQ and waits for the result -- see that class for how the
 * actual execution happens.
 */

@RestController
public final class SandboxController {

  /**
   * Sends a code submission to RabbitMQ and blocks until the result
   * comes back. This is the only way this controller triggers code
   * execution; it never calls Docker or {@link JavaRunnerService}
   * itself.
   */
  private final SandboxExecutionGateway sandboxExecutionGateway;

  /**
   * Registry containing all available coding problems.
   */
  private final ProblemRegistry problemRegistry;

  /**
   * Creates a sandbox controller.
   *
   * @param sandboxExecutionGateway used to run submitted code via
   *     RabbitMQ and wait for the result
   * @param registry registry containing available problems
   */
  public SandboxController(
      final SandboxExecutionGateway sandboxExecutionGateway,
      final ProblemRegistry registry
  ) {
    this.sandboxExecutionGateway = sandboxExecutionGateway;
    this.problemRegistry = registry;
  }

  /**
   * Executes Java source code submitted as-is from the sandbox page.
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
    return sandboxExecutionGateway.execute(sourceCode);
  }

  /**
   * Builds the full test source for a registered problem (the user's
   * solution plus the problem's hidden tests) and executes it.
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

    return sandboxExecutionGateway.execute(completeSource);
  }
}
