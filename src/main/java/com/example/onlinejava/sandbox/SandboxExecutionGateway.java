package com.example.onlinejava.sandbox;

import java.util.UUID;
import java.util.concurrent.Semaphore;
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
   * How long {@link #execute(String)} waits for a reply before giving
   * up. Spring AMQP's own default (5 seconds) is meant for ordinary
   * fast RPC calls; a submission can legitimately take up to
   * {@link JavaRunnerService#EXECUTION_TIMEOUT_SECONDS} to execute,
   * plus it may sit queued behind one other submission if both of
   * {@link JavaRunnerService#MAX_CONCURRENT_EXECUTIONS} execution
   * slots are already busy -- so the reply timeout has to cover one
   * full execution cycle waited-for, plus this request's own.
   *
   * <p>That's only a valid bound because {@link #admissionSlots} caps
   * how many requests can be waiting for a slot in the first place --
   * without that cap, a big enough burst could make a request wait
   * behind more than one full cycle (e.g. five requests arriving at
   * once against two execution slots means the fifth waits through
   * two cycles, not one), and no fixed timeout can cover an unbounded
   * queue.
   */
  private static final long REPLY_TIMEOUT_MILLIS =
      (2L * JavaRunnerService.EXECUTION_TIMEOUT_SECONDS + 10) * 1000L;

  /**
   * How many requests may be in flight through this gateway at once --
   * running or waiting for a free execution slot. Twice
   * {@link JavaRunnerService#MAX_CONCURRENT_EXECUTIONS}: the two
   * allowed to actually run, plus at most one more cycle's worth
   * waiting behind them, matching exactly what
   * {@link #REPLY_TIMEOUT_MILLIS} covers. A request that arrives once
   * this many are already in flight is rejected immediately with a
   * clear message instead of joining an ever-growing backlog that no
   * timeout could bound.
   */
  private static final int MAX_IN_FLIGHT_REQUESTS =
      2 * JavaRunnerService.MAX_CONCURRENT_EXECUTIONS;

  private final Semaphore admissionSlots = new Semaphore(MAX_IN_FLIGHT_REQUESTS);

  /**
   * Creates the gateway.
   *
   * @param rabbitTemplate the Spring-managed AMQP client used to send
   *     requests and wait for replies
   */
  public SandboxExecutionGateway(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
    this.rabbitTemplate.setReplyTimeout(REPLY_TIMEOUT_MILLIS);
  }

  /**
   * Sends {@code sourceCode} to the execution queue and blocks until
   * the listener on the other end replies with the console output.
   *
   * <p>Three failure cases are handled gracefully instead of throwing:
   * if too many requests are already in flight, if RabbitMQ itself
   * can't be reached, or if no reply arrives before the configured
   * timeout, this returns a plain error message instead of an
   * exception, so the caller always gets a {@code String} back.
   *
   * @param sourceCode complete Java source code to execute
   * @return the execution output, or a graceful error message if the
   *     request couldn't complete
   */
  String execute(String sourceCode) {
    if (!admissionSlots.tryAcquire()) {
      return "The sandbox is busy running other submissions. Please try again in a moment.";
    }

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
      admissionSlots.release();
      MDC.remove("correlationId");
    }
  }
}