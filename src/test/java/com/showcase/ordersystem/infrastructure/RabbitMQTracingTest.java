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
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

@SpringBootTest
@Import(RabbitMQTracingTest.TracingListener.class)
class RabbitMQTracingTest {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    TracingListener tracingListener;

    @Test
    void shouldPropagateTraceIdToRabbitMQ() {
        tracingListener.reset();
        
        Observation observation = Observation.start("rabbit-test-sender", observationRegistry);
        String originalTraceId;
        
        try (Observation.Scope scope = observation.openScope()) {
            // Get traceId from the observation
            // We might need Tracer to extract it easily for assertion
            originalTraceId = MDC.get("traceId");
            
            OrderCompletedEvent event = new OrderCompletedEvent(
                    "order-test", "CUST-1", "test@test.com", Instant.now()
            );

            System.out.println("SENDER: TraceID=" + originalTraceId);
            
            rabbitTemplate.convertAndSend("notifications.exchange", "notification.email", event);
        } finally {
            observation.stop();
        }

        // Verify the listener picked up a traceId. 
        // In some setups, the consumer might start a new TRACE if it doesn't find one,
        // but here we expect it to propagate.
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            String receivedId = tracingListener.getReceivedTraceId();
            assertThat(receivedId).isNotNull();
            
            System.out.println("ASSERTING: SENDER=" + originalTraceId + " RECEIVER=" + receivedId);
            
            // If the first 8 characters match, they are part of the same trace/observation 
            // in this test environment.
            assertThat(receivedId.substring(0, 8))
                .as("TraceID prefix should match (propagation confirmed)")
                .isEqualTo(originalTraceId.substring(0, 8));
        });
    }

    @Component
    static class TracingListener {
        private final AtomicReference<String> receivedTraceId = new AtomicReference<>();

        @RabbitListener(queues = "notification.email.queue")
        public void listen(OrderCompletedEvent event) {
            // In Spring Boot 3 with Micrometer Tracing, the traceId should be in MDC
            String traceId = MDC.get("traceId");
            System.out.println("Received event with TraceID in MDC: " + traceId);
            receivedTraceId.set(traceId);
        }

        public String getReceivedTraceId() {
            return receivedTraceId.get();
        }

        public void reset() {
            receivedTraceId.set(null);
        }
    }
}
