package de.caritas.cob.statisticsservice.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.caritas.cob.statisticsservice.api.model.EventType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/** RabbitMQ configuration for statistics. */
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

  private static final String CONNECTION_NAME = "StatisticsService";
  private static final String STATISTICS_EXCHANGE_NAME = "statistics.topic";
  private static final String DEAD_LETTER_EXCHANGE_NAME = "statistics.dead_letter";
  private static final String QUEUE_PREFIX = "statistics.";
  public static final String QUEUE_NAME_ASSIGN_SESSION = QUEUE_PREFIX + EventType.ASSIGN_SESSION;
  public static final String QUEUE_NAME_CREATE_MESSAGE = QUEUE_PREFIX + EventType.CREATE_MESSAGE;
  private static final String QUEUE_NAME_DEAD_LETTER_QUEUE = QUEUE_PREFIX + "dead_letter_queue";
  private static final String DEAD_LETTER_ROUTING_KEY = "DEAD_LETTER";
  private static final String X_DEAD_LETTER_EXCHANGE_HEADER = "x-dead-letter-exchange";
  private static final String X_DEAD_LETTER_ROUTING_KEY_HEADER = "x-dead-letter-routing-key";
  private final @NonNull CachingConnectionFactory connectionFactory;

  @Value("${spring.rabbitmq.listener.simple.retry.max-attempts}")
  private int retryMaxAttempts;
  @Value("${spring.rabbitmq.listener.simple.retry.initial-interval}")
  private int retryInitialInterval;
  @Value("${spring.rabbitmq.listener.simple.retry.max-interval}")
  private int retryMaxInterval;
  @Value("${spring.rabbitmq.listener.simple.retry.multiplier}")
  private int retryMultiplier;

  @Bean
  public Declarables topicBindings() {

    Queue deadLetterQueue = buildDeadLetterQueue();
    Queue assignSessionStatisticsEventQueue = buildQueue(QUEUE_NAME_ASSIGN_SESSION);
    Queue createMessageStatisticsEventQueue = buildQueue(QUEUE_NAME_CREATE_MESSAGE);

    DirectExchange deadLetterExchange = new DirectExchange(DEAD_LETTER_EXCHANGE_NAME, true, false);
    TopicExchange topicExchange = new TopicExchange(STATISTICS_EXCHANGE_NAME, true, false);

    return new Declarables(
        deadLetterQueue,
        deadLetterExchange,
        topicExchange,
        BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY),
        assignSessionStatisticsEventQueue,
        BindingBuilder.bind(assignSessionStatisticsEventQueue)
            .to(topicExchange)
            .with(EventType.ASSIGN_SESSION),
        createMessageStatisticsEventQueue,
        BindingBuilder.bind(createMessageStatisticsEventQueue)
            .to(topicExchange)
            .with(EventType.CREATE_MESSAGE));
  }

  private Queue buildQueue(String queueName) {
    return QueueBuilder.durable(queueName)
        .withArgument(X_DEAD_LETTER_EXCHANGE_HEADER, DEAD_LETTER_EXCHANGE_NAME)
        .withArgument(X_DEAD_LETTER_ROUTING_KEY_HEADER, DEAD_LETTER_ROUTING_KEY)
        .build();
  }

  private Queue buildDeadLetterQueue() {
    return QueueBuilder.durable(QUEUE_NAME_DEAD_LETTER_QUEUE).build();
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter(
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE));
  }

  @Bean
  public AmqpTemplate amqpTemplate(CachingConnectionFactory connectionFactory) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }

  @Bean
  RetryOperationsInterceptor statelessRetryOperationsInterceptor() {
    RepublishMessageRecoverer recoverer =
        new RepublishMessageRecoverer(
            amqpTemplate(connectionFactory), DEAD_LETTER_EXCHANGE_NAME, DEAD_LETTER_ROUTING_KEY);
    recoverer.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
    return RetryInterceptorBuilder.stateless()
        .maxAttempts(retryMaxAttempts)
        .backOffOptions(
            retryInitialInterval,
            retryMultiplier,
            retryMaxInterval)
        .recoverer(recoverer)
        .build();
  }

  @Bean
  public SmartInitializingSingleton reconfigureCf(final CachingConnectionFactory cf) {
    return () -> cf.setConnectionNameStrategy(f -> CONNECTION_NAME);
  }

  @Bean
  SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(
      CachingConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setAdviceChain(statelessRetryOperationsInterceptor());
    return factory;
  }
}
