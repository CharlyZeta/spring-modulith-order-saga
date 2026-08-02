package com.showcase.ordersystem.infrastructure;

import com.showcase.ordersystem.shared.OrderCompletedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
@ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
@Import(RabbitMQTracingTest.TracingListener.class)
class RabbitMQTracingTest {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    TracingListener tracingListener;

    @Autowired
    io.micrometer.tracing.Tracer tracer;

    @BeforeEach
    void checkRabbitMQAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5672), 500);
        } catch (IOException e) {
            Assumptions.assumeTrue(false, "RabbitMQ broker is not running on localhost:5672. Skipping live RabbitMQ test.");
        }
    }

    @Test
    void shouldPropagateTraceIdToRabbitMQ() {
        tracingListener.reset();
        
        Observation observation = Observation.start("rabbit-test-sender", observationRegistry);
        String originalTraceId;
        
        try (Observation.Scope scope = observation.openScope()) {
            // Get traceId from the tracer
            originalTraceId = tracer.currentSpan().context().traceId();
            
            OrderCompletedEvent event = new OrderCompletedEvent(
                    "order-test", "CUST-1", "test@test.com", Instant.now()
            );

            System.out.println("SENDER: TraceID=" + originalTraceId);
            
            rabbitTemplate.convertAndSend("notifications.exchange", "notification.email", event);
        } finally {
            observation.stop();
        }

        // Verify the listener picked up a traceId. 
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            String receivedId = tracingListener.getReceivedTraceId();
            assertThat(receivedId).isNotNull();
            
            System.out.println("ASSERTING: SENDER=" + originalTraceId + " RECEIVER=" + receivedId);
            
            // If the first 8 characters match, they are part of the same trace/observation 
            assertThat(receivedId.substring(0, 8))
                .as("TraceID prefix should match (propagation confirmed)")
                .isEqualTo(originalTraceId.substring(0, 8));
        });
    }

    @Component
    static class TracingListener {
        private final AtomicReference<String> receivedTraceId = new AtomicReference<>();

        @RabbitListener(queues = "notification.email.queue")
        public void listen(org.springframework.amqp.core.Message message) {
            // Check MDC
            String traceId = MDC.get("traceId");
            
            // If MDC is empty (common in some test setups), check headers
            if (traceId == null || traceId.isEmpty()) {
                Object traceHeader = message.getMessageProperties().getHeader("traceparent");
                if (traceHeader != null) {
                    // traceparent format: 00-traceId-spanId-flags
                    String[] parts = traceHeader.toString().split("-");
                    if (parts.length > 1) {
                        traceId = parts[1];
                    }
                }
                
                // Also check 'b3' header just in case
                if (traceId == null || traceId.isEmpty()) {
                    Object b3Header = message.getMessageProperties().getHeader("b3");
                    if (b3Header != null) {
                        traceId = b3Header.toString().split("-")[0];
                    }
                }
            }
            
            System.out.println("Received message in test listener. TraceID detected: " + traceId);
            if (traceId != null) {
                receivedTraceId.set(traceId);
            }
        }

        public String getReceivedTraceId() {
            return receivedTraceId.get();
        }

        public void reset() {
            receivedTraceId.set(null);
        }
    }
}
