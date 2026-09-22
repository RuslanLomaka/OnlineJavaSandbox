package com.example.onlinejava;

import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tweaks the auto-configured {@code RabbitTemplate} so it uses the
 * correlation ID {@link SandboxExecutionGateway} sets, instead of
 * silently replacing it with one of its own.
 *
 * <p>By default, {@code RabbitTemplate} generates its own random
 * correlation ID for every {@code convertSendAndReceive} call and
 * overwrites whatever was set on the outgoing message -- it needs a
 * guaranteed-unique ID for its own internal reply-tracking, and
 * doesn't trust the caller to provide one. Since
 * {@link SandboxExecutionGateway} already generates a fresh
 * {@code UUID} per request for exactly that purpose, enabling
 * {@code userCorrelationId} tells the template to trust and reuse
 * that ID instead, so the same ID appears in both the gateway's and
 * the listener's logs.
 */
@Configuration
public class RabbitMqConfig {

  /**
   * Enables {@code userCorrelationId} on the auto-configured
   * {@code RabbitTemplate}.
   *
   * @return the customizer Spring Boot applies to the template it builds
   */
  @Bean
  public RabbitTemplateCustomizer userCorrelationIdCustomizer() {
    return rabbitTemplate -> rabbitTemplate.setUserCorrelationId(true);
  }
}
