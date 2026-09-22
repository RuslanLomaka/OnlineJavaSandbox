package com.example.onlinejava.sandbox;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Entry point that hands a code submission off to RabbitMQ instead of
 * running it directly, then waits for the result.
 *
 * <p>This is the "producer" side of an RPC-over-RabbitMQ pattern: a
 * request/reply call made through a message queue instead of a normal
 * method call. It never touches Docker or {@link JavaRunnerService}
 * itself -- it only sends a message and blocks until a reply with a
 * matching correlation ID comes back. {@link SandboxExecutionListener}
 * is the other half: it receives the message, does the actual work,
 * and sends the reply.
 */
@Component
public class SandboxExecutionGateway {

  private static final Logger log = LoggerFactory.getLogger(SandboxExecutionGateway.class);
  /**
   * Spring's managed AMQP client. Auto-created because
   * {@code spring-boot-starter-amqp} is on the classpath; injected here
   * rather than constructed manually so it uses the real connection
   * (host/credentials from {@code application-*.properties}).
   */
  private final RabbitTemplate rabbitTemplate;

  /**
   * Creates the gateway.
   *
   * @param rabbitTemplate the Spring-managed AMQP client used to send
   *     requests and wait for replies
   */
  public SandboxExecutionGateway(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Sends {@code sourceCode} to the execution queue and blocks until
   * the listener on the other end replies with the console output.
   *
   * <p>Two failure cases are handled gracefully instead of throwing:
   * if RabbitMQ itself can't be reached, or if no reply arrives before
   * the configured timeout, this returns a plain error message instead
   * of an exception, so the caller always gets a {@code String} back.
   *
   * @param sourceCode complete Java source code to execute
   * @return the execution output, or a graceful error message if the
   *     request couldn't complete
   */
  String execute(String sourceCode) {
    // A random ID unique to this one call. It travels inside the AMQP
    // message as the "correlation ID" so that when a reply eventually
    // comes back, Spring knows it belongs to *this* execute() call and
    // not some other request that happens to be in flight at the same
    // time. Generated fresh every call (a local variable, not a field)
    // because this bean is a singleton shared by every request.
    String id = UUID
        .randomUUID()
        .toString();
    MDC.put("correlationId", id);
    log.info("Request received");
    // Name of the queue the listener is subscribed to. Both sides must
    // use the exact same literal for a message to actually be picked up.
    String queueName = "sandbox.execution.requests";

    // Without RabbitMqConfig's userCorrelationId customizer,
    // RabbitTemplate silently overwrites the id we set below with a
    // random one of its own for internal reply-tracking -- see that
    // class's Javadoc for why.

    // convertSendAndReceive does three things in one call:
    //  1. Wraps sourceCode into an AMQP message.
    //  2. Sends it to queueName.
    //  3. Blocks this thread until a reply tagged with the same
    //     correlation ID arrives (or times out, returning null).
    // The lambda is a MessagePostProcessor: Spring builds the message,
    // hands it to us right before sending, and we stamp our id onto it
    // as the correlation ID before it actually goes out.

    Object rawReply;
    try {
      rawReply = rabbitTemplate.convertSendAndReceive(
          queueName,
          sourceCode,
          message -> {
            message
                .getMessageProperties()
                .setCorrelationId(id);
            return message;
          }
      );
      if (rawReply == null) {
        return "Execution timed out.";
      }
      log.info("Published, got reply");
      return (String) rawReply;
    } catch (AmqpException exception) {
      return "Could not reach the execution queue.";
    } finally {
      MDC.remove("correlationId");
    }
  }
}