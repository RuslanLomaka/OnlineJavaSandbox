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

@RestController
public class SandboxController {

    private final JavaRunnerService javaRunnerService;
    private final ProblemRegistry problemRegistry;

    public SandboxController(
            JavaRunnerService javaRunnerService,
            ProblemRegistry problemRegistry
    ) {
        this.javaRunnerService = javaRunnerService;
        this.problemRegistry = problemRegistry;
    }

    @PostMapping(
            value = "/sandbox/run",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String run(@RequestBody String sourceCode) {
        return javaRunnerService.run(sourceCode);
    }

    // Builds and executes the test source for a registered problem.
    @PostMapping(
            value = "/problems/{slug}/run",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String runProblem(
            @PathVariable String slug,
            @RequestBody String solutionCode
    ) {
        ProblemDefinition problemDefinition =
                problemRegistry.getProblem(slug);

        if (problemDefinition == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Problem not found"
            );
        }

        String completeSource =
                problemDefinition.buildTestSource(solutionCode);

        return javaRunnerService.run(completeSource);
    }
}