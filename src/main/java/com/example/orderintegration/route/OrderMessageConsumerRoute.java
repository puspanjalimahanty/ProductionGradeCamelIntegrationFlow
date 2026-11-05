package com.example.orderintegration.route;

import java.util.Set;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.example.orderintegration.dto.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
public class OrderMessageConsumerRoute extends RouteBuilder {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public OrderMessageConsumerRoute(
            ObjectMapper objectMapper,
            Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public void configure() {

        errorHandler(
            deadLetterChannel("jms:queue:order.dlq")
                .maximumRedeliveries(3)
                .redeliveryDelay(2000)
                .useOriginalMessage()
        );
        
        onException(Exception.class)
        .log(org.apache.camel.LoggingLevel.ERROR,
             "Artemis-to-Kafka failed: ${exception.message}")
        .log(org.apache.camel.LoggingLevel.ERROR,
             "${exception.stacktrace}")
        .handled(false);
        

        from("jms:queue:order.queue")
            .routeId("order-message-consumer")
            .convertBodyTo(String.class)
            .setHeader("correlationId", header("JMSCorrelationID"))
            .setHeader("correlationId", header("JMSCorrelationID"))
            .setHeader("kafka.CorrelationId", header("correlationId"))
            .log("Received order message. Correlation ID: ${header.correlationId}")

            .process(exchange -> {
                String json =
                    exchange.getMessage().getBody(String.class);

                OrderRequest request =
                    objectMapper.readValue(json, OrderRequest.class);

                Set<ConstraintViolation<OrderRequest>> violations =
                    validator.validate(request);

                if (!violations.isEmpty()) {
                    ConstraintViolation<OrderRequest> violation =
                        violations.iterator().next();

                    throw new IllegalArgumentException(
                        "Invalid order: "
                        + violation.getPropertyPath()
                        + " "
                        + violation.getMessage()
                    );
                }

                exchange.getMessage()
                    .setHeader("orderId", request.getOrderId());

                // Keep the validated order as a JSON string for Kafka.
                exchange.getMessage()
                    .setBody(objectMapper.writeValueAsString(request));
            })

            .log("Order validated. Order ID: ${header.orderId}")

            // Forward the validated order to Kafka.
            .process(exchange -> {
                String correlationId =
                    exchange.getMessage().getHeader("correlationId", String.class);

                if (correlationId != null && !correlationId.isBlank()) {
                    exchange.getMessage().setHeader(
                        "kafka.HEADER_CorrelationId", correlationId);
                }
            })
            .to("kafka:{{app.kafka.order-topic}}")

            .log("Order published to Kafka. Order ID: ${header.orderId}, Correlation ID: ${header.correlationId}");
    }
}
