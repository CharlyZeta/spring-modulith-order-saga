package com.showcase.ordersystem.notifications;

import com.showcase.ordersystem.shared.events.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldSendNotificationToRabbitMQ() {
        // Arrange
        OrderCompletedEvent event = new OrderCompletedEvent(
                "order-1", "CUST-1", "customer@test.com", Instant.now()
        );

        // Act
        notificationService.onOrderCompleted(event);

        // Assert
        ArgumentCaptor<NotificationService.NotificationMessage> messageCaptor = 
                ArgumentCaptor.forClass(NotificationService.NotificationMessage.class);

        verify(rabbitTemplate).convertAndSend(
                eq("notifications.exchange"),
                eq("notification.email"),
                messageCaptor.capture()
        );

        NotificationService.NotificationMessage message = messageCaptor.getValue();
        assertThat(message.recipientEmail()).isEqualTo("customer@test.com");
        assertThat(message.subject()).isEqualTo("Order Confirmation");
        assertThat(message.body()).contains("order-1");
    }
}
