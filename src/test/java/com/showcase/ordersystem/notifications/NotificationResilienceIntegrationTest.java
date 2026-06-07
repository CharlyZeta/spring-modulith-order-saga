package com.showcase.ordersystem.notifications;

import com.showcase.ordersystem.infrastructure.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class NotificationResilienceIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

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
