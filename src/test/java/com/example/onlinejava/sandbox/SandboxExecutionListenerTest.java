package com.example.onlinejava.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Unit tests for {@link SandboxExecutionListener}.
 */
class SandboxExecutionListenerTest {

  @Test
  void concurrencyMatchesJavaRunnerServicesExecutionSlotLimit() throws NoSuchMethodException {
    final Method method = SandboxExecutionListener.class.getDeclaredMethod(
        "runNextRequestFromQueue", String.class, String.class);
    final RabbitListener annotation = method.getAnnotation(RabbitListener.class);

    assertThat(annotation.concurrency())
        .isEqualTo(String.valueOf(JavaRunnerService.MAX_CONCURRENT_EXECUTIONS));
  }

  @Test
  void doesNotLeakTheRawExceptionMessageIntoTheReply() {
    final JavaRunnerService javaRunnerService = mock(JavaRunnerService.class);
    when(javaRunnerService.run("boom")).thenThrow(new RuntimeException(
        "/tmp/online-java-runs/java-sandbox-xyz/Main.java: Permission denied"));
    final SandboxExecutionListener listener = new SandboxExecutionListener(javaRunnerService);

    final String result = listener.runNextRequestFromQueue("boom", "test-correlation-id");

    assertThat(result)
        .doesNotContain("/tmp/online-java-runs")
        .doesNotContain("Permission denied")
        .isEqualTo("Execution failed due to an internal error. Please try again.");
  }
}
