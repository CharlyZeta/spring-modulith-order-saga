package com.showcase.ordersystem.notifications;

import com.showcase.ordersystem.shared.events.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Public API of the Notifications module.
 * Listens to OrderCompletedEvent and sends notifications via RabbitMQ.
 * 
 * This demonstrates:
 * - Spring Modulith event listeners
 * - Integration with RabbitMQ for external messaging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Listens to OrderCompletedEvent from the Orders module.
     * Sends notification via RabbitMQ to external notification service.
     */
    @ApplicationModuleListener
    void onOrderCompleted(OrderCompletedEvent event) {
        log.info("Received order completed event for order: {}", event.orderId());

        // Create notification message
        NotificationMessage message = new NotificationMessage(
                event.customerId(),
                event.customerEmail(),
                "Order Confirmation",
                String.format(
                        "Your order %s has been successfully completed and will be shipped soon!",
                        event.orderId()
                ),
                Instant.now()
        );

        // Send to RabbitMQ (external notification service would consume this)
        try {
            rabbitTemplate.convertAndSend(
                    "notifications.exchange",
                    "notification.email",
                    message
            );
            log.info("Sent notification to RabbitMQ for customer: {}", event.customerEmail());
        } catch (Exception e) {
            log.error("Failed to send notification for order: {}", event.orderId(), e);
        }
    }

    /**
     * Message format for RabbitMQ notifications.
     */
    record NotificationMessage(
            String customerId,
            String recipientEmail,
            String subject,
            String body,
            Instant timestamp
    ) {}
}
