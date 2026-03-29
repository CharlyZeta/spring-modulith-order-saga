package com.showcase.ordersystem.infrastructure;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for notification messaging.
 *
 * Creates:
 * - Topic exchange for notifications
 * - Queue for email notifications
 * - Dead letter queue for failed messages
 * - Binding between exchange and queue
 */
@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATIONS_EXCHANGE = "notifications.exchange";
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String EMAIL_ROUTING_KEY = "notification.email";
    public static final String DLX_EXCHANGE = "notifications.dlx";
    public static final String DLQ_QUEUE = "notification.email.dlq";
    public static final String DLQ_ROUTING_KEY = "notification.email.dlq";

    /**
     * Topic exchange for routing notification messages.
     */
    @Bean
    public TopicExchange notificationsExchange() {
        return ExchangeBuilder
                .topicExchange(NOTIFICATIONS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Queue for email notifications with dead letter configuration.
     */
    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder
                .durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Binding between notifications exchange and email queue.
     */
    @Bean
    public Binding emailNotificationBinding(Queue emailNotificationQueue, TopicExchange notificationsExchange) {
        return BindingBuilder
                .bind(emailNotificationQueue)
                .to(notificationsExchange)
                .with(EMAIL_ROUTING_KEY);
    }

    /**
     * Dead Letter Exchange for failed messages.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Dead Letter Queue for permanently failed messages.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(DLQ_QUEUE)
                .build();
    }

    /**
     * Binding between DLX and DLQ.
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(DLQ_ROUTING_KEY);
    }

    /**
     * JSON message converter for serializing/deserializing messages.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
