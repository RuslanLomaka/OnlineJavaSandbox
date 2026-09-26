package com.example.onlinejava.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Unit tests for {@link SandboxExecutionGateway}.
 */
class SandboxExecutionGatewayTest {

  private static final int MAX_IN_FLIGHT = 2 * JavaRunnerService.MAX_CONCURRENT_EXECUTIONS;

  @Test
  void setsReplyTimeoutLongerThanTwoFullExecutionCycles() {
    final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    new SandboxExecutionGateway(rabbitTemplate);

    final long twoExecutionCyclesMillis = 2L * JavaRunnerService.EXECUTION_TIMEOUT_SECONDS * 1000L;
    verify(rabbitTemplate).setReplyTimeout(
        longThat(millis -> millis > twoExecutionCyclesMillis));
  }

  @Test
  void rejectsRequestsBeyondTheAdmissionLimitWithoutTouchingRabbitMq() throws InterruptedException {
    final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    final CountDownLatch allInFlight = new CountDownLatch(MAX_IN_FLIGHT);
    final CountDownLatch releaseInFlight = new CountDownLatch(1);
    when(rabbitTemplate.convertSendAndReceive(
        anyString(), any(Object.class), any(MessagePostProcessor.class)))
        .thenAnswer(invocation -> {
          allInFlight.countDown();
          releaseInFlight.await();
          return "ok";
        });
    final SandboxExecutionGateway gateway = new SandboxExecutionGateway(rabbitTemplate);
    final ExecutorService pool = Executors.newFixedThreadPool(MAX_IN_FLIGHT);

    try {
      for (int i = 0; i < MAX_IN_FLIGHT; i++) {
        pool.submit(() -> gateway.execute("code"));
      }
      assertThat(allInFlight.await(5, TimeUnit.SECONDS)).as("every slot filled").isTrue();

      final String result = gateway.execute("one too many");

      assertThat(result).isEqualTo(
          "The sandbox is busy running other submissions. Please try again in a moment.");
      verify(rabbitTemplate, times(MAX_IN_FLIGHT)).convertSendAndReceive(
          anyString(), any(Object.class), any(MessagePostProcessor.class));
    } finally {
      releaseInFlight.countDown();
      pool.shutdown();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).as("pool drained").isTrue();
    }
  }
}
