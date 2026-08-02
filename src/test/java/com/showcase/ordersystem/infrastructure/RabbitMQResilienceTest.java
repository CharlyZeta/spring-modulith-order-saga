package com.showcase.ordersystem.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@SpringBootTest
@ActiveProfiles("test")
@Import({RabbitMQResilienceTest.FlakyListener.class, RabbitMQResilienceTest.DlqConsumer.class})
class RabbitMQResilienceTest {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    DlqConsumer dlqConsumer;

    @BeforeEach
    void checkRabbitMQAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5672), 500);
        } catch (IOException e) {
            Assumptions.assumeTrue(false, "RabbitMQ broker is not running on localhost:5672. Skipping live RabbitMQ test.");
        }
    }

    @Test
    void shouldSendToDlqAfterRetryExhaustion() {
        dlqConsumer.reset();
        
        String payload = "{\"message\": \"fail-me\"}";
        
        // Send directly to the queue that has a flaky listener
        rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_QUEUE, payload);

        // Wait for it to appear in DLQ
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(dlqConsumer.getDlqCount()).isGreaterThan(0);
        });
        
        System.out.println("Verified message in DLQ after failure.");
    }

    @Component
    static class FlakyListener {
        @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
        public void listen(String message) {
            if (message.contains("fail-me")) {
                System.out.println("Intentional failure for message: " + message);
                throw new RuntimeException("Simulated failure");
            }
        }
    }

    @Component
    static class DlqConsumer {
        private final AtomicInteger dlqCount = new AtomicInteger(0);

        @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
        public void listenDlq(Message message) {
            System.out.println("Received message in DLQ!");
            dlqCount.incrementAndGet();
        }

        public int getDlqCount() {
            return dlqCount.get();
        }

        public void reset() {
            dlqCount.set(0);
        }
    }
}
