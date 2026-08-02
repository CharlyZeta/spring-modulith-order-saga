package com.showcase.ordersystem.notifications;

import com.showcase.ordersystem.infrastructure.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class NotificationResilienceIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void checkRabbitMQAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5672), 500);
        } catch (IOException e) {
            Assumptions.assumeTrue(false, "RabbitMQ broker is not running on localhost:5672. Skipping live RabbitMQ test.");
        }
    }

    @Test
    void shouldSendToQueueSuccessfully() {
        // Creamos un mensaje que simulará ser una notificación
        NotificationService.NotificationMessage message = new NotificationService.NotificationMessage(
                "CUST-TEST", "test@test.com", "Test Queue", "Testing basic connectivity", Instant.now()
        );

        // Enviamos el mensaje a la cola
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATIONS_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, message);

        // Verificamos que al menos un mensaje llegó (podría ser consumido por el listener real si está activo, 
        // pero aquí solo probamos que el envío no falla y la infra está up)
        System.out.println("Message successfully sent to exchange: " + RabbitMQConfig.NOTIFICATIONS_EXCHANGE);
    }
}
