package com.example.onlinejava;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SandboxController {

    private final JavaRunnerService javaRunnerService;

    public SandboxController(JavaRunnerService javaRunnerService) {
        this.javaRunnerService = javaRunnerService;
    }

    @PostMapping(
            value = "/sandbox/run",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String run(@RequestBody String sourceCode) {
        return javaRunnerService.run(sourceCode);
    }
}