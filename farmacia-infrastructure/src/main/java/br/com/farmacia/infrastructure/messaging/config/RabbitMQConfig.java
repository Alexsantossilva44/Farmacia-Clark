package br.com.farmacia.infrastructure.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração RabbitMQ para integração SNGPC assíncrona.
 *
 * <p>Topologia de filas:</p>
 * <pre>
 *   [Venda Controlado]
 *         │
 *         ▼
 *   Exchange: sngpc.exchange (Direct)
 *         │
 *         ├──► Queue: sngpc.registros         (envio principal)
 *         │              │ falha               TTL: sem expiração
 *         │              └──► Exchange: sngpc.dlx
 *         │                         │
 *         │                         └──► Queue: sngpc.dlq (Dead Letter)
 *         │                                      └── retry após 5 min
 *         │
 *         └──► Queue: sngpc.reprocessamento   (retry manual/scheduler)
 * </pre>
 */
@Configuration
public class RabbitMQConfig {

    // ── Nomes das exchanges e filas ───────────────────────────────────────────

    public static final String SNGPC_EXCHANGE         = "sngpc.exchange";
    public static final String SNGPC_DLX              = "sngpc.dlx";

    public static final String SNGPC_QUEUE            = "sngpc.registros";
    public static final String SNGPC_DLQ              = "sngpc.dlq";
    public static final String SNGPC_REPROCESS_QUEUE  = "sngpc.reprocessamento";

    public static final String SNGPC_ROUTING_KEY      = "sngpc.novo";
    public static final String SNGPC_DLQ_ROUTING_KEY  = "sngpc.morto";

    // Tempo de espera antes do retry: 5 minutos (em milissegundos)
    private static final int DLQ_TTL_MS = 5 * 60 * 1000;

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Bean
    public DirectExchange sngpcExchange() {
        return ExchangeBuilder.directExchange(SNGPC_EXCHANGE)
            .durable(true)
            .build();
    }

    @Bean
    public DirectExchange sngpcDeadLetterExchange() {
        return ExchangeBuilder.directExchange(SNGPC_DLX)
            .durable(true)
            .build();
    }

    // ── Filas ─────────────────────────────────────────────────────────────────

    /**
     * Fila principal de envio ao SNGPC.
     * Mensagens rejeitadas (NACK) vão para a DLX após {@code x-message-ttl}.
     */
    @Bean
    public Queue sngpcQueue() {
        return QueueBuilder.durable(SNGPC_QUEUE)
            .withArgument("x-dead-letter-exchange", SNGPC_DLX)
            .withArgument("x-dead-letter-routing-key", SNGPC_DLQ_ROUTING_KEY)
            .build();
    }

    /**
     * Dead Letter Queue — armazena mensagens com falha para reprocessamento.
     * Após TTL, a mensagem volta automaticamente para a fila principal.
     */
    @Bean
    public Queue sngpcDeadLetterQueue() {
        return QueueBuilder.durable(SNGPC_DLQ)
            .withArgument("x-message-ttl", DLQ_TTL_MS)
            .withArgument("x-dead-letter-exchange", SNGPC_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", SNGPC_ROUTING_KEY)
            .build();
    }

    /** Fila de reprocessamento manual (usada pelo scheduler de retry). */
    @Bean
    public Queue sngpcReprocessQueue() {
        return QueueBuilder.durable(SNGPC_REPROCESS_QUEUE).build();
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    @Bean
    public Binding sngpcBinding() {
        return BindingBuilder.bind(sngpcQueue())
            .to(sngpcExchange())
            .with(SNGPC_ROUTING_KEY);
    }

    @Bean
    public Binding sngpcDlqBinding() {
        return BindingBuilder.bind(sngpcDeadLetterQueue())
            .to(sngpcDeadLetterExchange())
            .with(SNGPC_DLQ_ROUTING_KEY);
    }

    // ── Serialização JSON ─────────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
