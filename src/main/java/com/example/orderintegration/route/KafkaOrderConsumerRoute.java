package com.example.orderintegration.route;

import java.util.Set;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.example.orderintegration.dto.OrderRequest;
import com.example.orderintegration.service.IntegrationErrorService;
import com.example.orderintegration.service.ProcessedKafkaOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
public class KafkaOrderConsumerRoute extends RouteBuilder {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ProcessedKafkaOrderService processedOrderService;
    private final IntegrationErrorService integrationErrorService;

    public KafkaOrderConsumerRoute(
            ObjectMapper objectMapper,
            Validator validator,
            ProcessedKafkaOrderService processedOrderService,
            IntegrationErrorService integrationErrorService) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.processedOrderService = processedOrderService;
        this.integrationErrorService = integrationErrorService;
    }

    @Override
    public void configure() {
    	
    	onException(Exception.class)
        .maximumRedeliveries(2)
        .redeliveryDelay(1000)
        .process(exchange -> {
            String orderId = exchange.getMessage()
                    .getHeader("orderId", String.class);

            String correlationId = exchange.getMessage()
                    .getHeader("correlationId", String.class);

            Exception exception = exchange.getProperty(
                    org.apache.camel.Exchange.EXCEPTION_CAUGHT,
                    Exception.class);

            integrationErrorService.recordError(
                    orderId,
                    "KAFKA",
                    correlationId,
                    exception != null ? exception.getMessage() : "Unknown error"
            );
        })
        .handled(true)
        .log(LoggingLevel.ERROR,
            "Kafka order processing failed: ${exception.message}")
        .setHeader("errorMessage", simple("${exception.message}"))
        .to("kafka:{{app.kafka.error-topic}}");

        from("kafka:{{app.kafka.order-topic}}"
                + "?groupId=order-downstream-consumer")
            .routeId("kafka-order-consumer")
            .convertBodyTo(String.class)
            .process(exchange -> {
                String json =
                    exchange.getMessage().getBody(String.class);

                OrderRequest request =
                    objectMapper.readValue(json, OrderRequest.class);

                Set<ConstraintViolation<OrderRequest>> violations =
                    validator.validate(request);
                
                String correlationId = exchange.getMessage()
                        .getHeader("kafka.HEADER_CorrelationId", String.class);

                exchange.getMessage().setHeader("correlationId", correlationId);
                boolean isNew = processedOrderService.markIfNotProcessed(request.getOrderId());

                if (!isNew) {
                    exchange.getMessage().setHeader("duplicateOrder", true);
                } else {
                    exchange.getMessage().setHeader("duplicateOrder", false);
                }

                exchange.getMessage().setHeader("orderId", request.getOrderId());
                exchange.getMessage().setHeader(
                	    "processingTime",
                	    java.time.LocalDateTime.now().toString()
                	);

                if (!violations.isEmpty()) {
                    ConstraintViolation<OrderRequest> violation =
                        violations.iterator().next();

                    throw new IllegalArgumentException(
                        "Invalid Kafka order: "
                        + violation.getPropertyPath()
                        + " "
                        + violation.getMessage()
                    );
                }

                exchange.getMessage()
                    .setHeader("orderId", request.getOrderId());
            })
            
            .choice()
            .when(header("duplicateOrder").isEqualTo(true))
            .log("Kafka order processed. Order ID: ${header.orderId}, Correlation ID: ${header.correlationId}")
                .stop()
            .otherwise()
                .log("Kafka order validated and processed successfully. Order ID: ${header.orderId}")
        .end();
    }
}
