package com.example.onlinejava.sandbox;

import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Unit tests for {@link SandboxExecutionGateway}.
 */
class SandboxExecutionGatewayTest {

  @Test
  void setsReplyTimeoutLongerThanTwoFullExecutionCycles() {
    final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    new SandboxExecutionGateway(rabbitTemplate);

    final long twoExecutionCyclesMillis = 2L * JavaRunnerService.EXECUTION_TIMEOUT_SECONDS * 1000L;
    verify(rabbitTemplate).setReplyTimeout(
        longThat(millis -> millis > twoExecutionCyclesMillis));
  }
}
