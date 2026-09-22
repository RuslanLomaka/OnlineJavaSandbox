package com.example.onlinejava;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Picks up code-execution requests from the RabbitMQ queue and runs
 * them through {@link JavaRunnerService}, exactly as
 * {@link SandboxController} used to call it directly.
 *
 * <p>This is the "consumer" side of the RabbitMQ RPC pattern. It reads
 * back the correlation ID {@link SandboxExecutionGateway} stamped on
 * the message and tags its own logs with it, so a request's whole
 * journey -- gateway and listener -- is grep-able as one ID. It never
 * has to manage replies itself, though: Spring handles that
 * automatically because this method has a return value and the
 * incoming message came from
 * {@link SandboxExecutionGateway#execute(String)}'s
 * {@code convertSendAndReceive} call.
 */
@Component
public class SandboxExecutionListener {

  /**
   * The unchanged service that actually shells out to Docker.
   */
  private final JavaRunnerService javaRunnerService;

  /**
   * Logs unexpected failures from {@link #runNextRequestFromQueue}.
   * Normal execution failures (bad user code, timeouts, etc.) are
   * already turned into readable output by {@link JavaRunnerService}
   * itself -- this only fires for something going wrong in the
   * listener's own handling of the message.
   */
  private static final Logger log = LoggerFactory.getLogger(SandboxExecutionListener.class);

  /**
   * Creates the listener.
   *
   * @param javaRunnerService service responsible for executing Java
   *     code inside a Docker container
   */
  SandboxExecutionListener(JavaRunnerService javaRunnerService) {
    this.javaRunnerService = javaRunnerService;
  }

  /**
   * Runs one queued code submission and returns its output.
   *
   * <p>{@code queuesToDeclare} both subscribes to the queue and makes
   * sure it actually exists in RabbitMQ -- without it, a message sent
   * to a queue nobody has declared would just be dropped. The queue
   * name here must match {@link SandboxExecutionGateway}'s literal
   * exactly, since that's how a message ends up here at all.
   *
   * <p>Returning a {@code String} (rather than {@code void}) is what
   * tells Spring to automatically send that value back to whichever
   * {@code convertSendAndReceive} call is waiting for it -- no manual
   * reply-sending code needed.
   *
   * <p>If something throws while handling the message (as opposed to
   * the submitted code simply failing to compile or run, which
   * {@link JavaRunnerService} already reports as normal output), it's
   * caught here, logged, and turned into a reply string instead of
   * being rethrown -- letting the exception escape this method would
   * mean the waiting caller never gets a reply at all, just a timeout.
   *
   * @param sourceCode the complete Java source pulled out of the
   *     incoming message body
   * @param correlationId the same ID {@link SandboxExecutionGateway}
   *     generated for this request, read back off the AMQP message
   *     header so this class's logs (and, since it calls
   *     {@link JavaRunnerService} on this same thread, that class's
   *     logs too) carry it
   * @return the execution output, sent back as the reply
   */
  @RabbitListener(queuesToDeclare = @Queue("sandbox.execution.requests"))
  String runNextRequestFromQueue(
      String sourceCode,
      @Header(AmqpHeaders.CORRELATION_ID) String correlationId
  ) {
    MDC.put("correlationId", correlationId);
    try {
      log.info("Consumed");

      String result;
      try {
        result = javaRunnerService.run(sourceCode);
      } catch (Exception exception) {
        log.error("Execution failed: {}", exception.getMessage());
        result = "Execution failed: " + exception.getMessage();
      }

      return result;
    } finally {
      MDC.remove("correlationId");
    }
  }
}
