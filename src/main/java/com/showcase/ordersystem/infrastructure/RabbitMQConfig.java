package com.showcase.ordersystem.infrastructure;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration.
 * Defines exchanges, queues, and bindings for cross-module notifications.
 */
@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATIONS_EXCHANGE = "notifications.exchange";
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String EMAIL_ROUTING_KEY = "notification.email";
    public static final String NOTIFICATIONS_DLX = "notifications.dlx";
    public static final String DLQ_QUEUE = "notification.email.dlq";

    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATIONS_DLX)
                .withArgument("x-dead-letter-routing-key", "notification.email.dlq")
                .build();
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationsExchange).with("notification.email");
    }

    @Bean
    public TopicExchange notificationsDlx() {
        return new TopicExchange(NOTIFICATIONS_DLX);
    }

    @Bean
    public Queue dlqQueue() {
        return new Queue(DLQ_QUEUE);
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, TopicExchange notificationsDlx) {
        return BindingBuilder.bind(dlqQueue).to(notificationsDlx).with("notification.email.dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setObservationEnabled(true);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                       MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setObservationEnabled(true);
        return template;
    }
}
