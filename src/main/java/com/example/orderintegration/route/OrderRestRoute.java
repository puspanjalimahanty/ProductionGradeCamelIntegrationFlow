package com.example.orderintegration.route;

import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.example.orderintegration.dto.OrderRequest;
import com.example.orderintegration.entity.OrderEntity;
import com.example.orderintegration.exception.DuplicateOrderException;
import com.example.orderintegration.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Component
public class OrderRestRoute extends RouteBuilder {

    private final OrderService orderService;
    private final Validator validator;
    private final ProducerTemplate producerTemplate;
    private final ObjectMapper objectMapper;

    public OrderRestRoute(
            OrderService orderService,
            Validator validator,
            ProducerTemplate producerTemplate,
            ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.validator = validator;
        this.producerTemplate = producerTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure() {

        onException(DuplicateOrderException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(409))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("{\"error\":\"Order already exists\"}"));

        onException(ConstraintViolationException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("{\"error\":\"Invalid order request\"}"));

        onException(IllegalArgumentException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("{\"error\":\"Order not found\"}"));

        restConfiguration()
            .component("platform-http")
            .bindingMode(
                org.apache.camel.model.rest.RestBindingMode.json);

        rest("/api/orders")
            .post()
                .type(OrderRequest.class)
                .to("direct:createOrder")
            .get("/{orderId}")
                .outType(OrderEntity.class)
                .to("direct:getOrder");

        // Create order, save to H2, then publish to Artemis
        from("direct:createOrder")
            .routeId("create-order")
            .process(exchange -> {
                OrderRequest request = exchange.getMessage()
                    .getBody(OrderRequest.class);

                Set<ConstraintViolation<OrderRequest>> violations =
                    validator.validate(request);

                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }

                OrderEntity saved =
                    orderService.createOrder(request, "REST");
                String orderJson = objectMapper.copy()
                	    .findAndRegisterModules()
                	    .writeValueAsString(request);

                	producerTemplate.sendBody(
                	    "jms:queue:order.queue",
                	    orderJson
                	);

                // Convert the saved order into JSON
                ObjectMapper mapper = objectMapper.copy()
                	    .findAndRegisterModules();

                	

                exchange.getMessage().setBody(saved);
                exchange.getMessage().setHeader(
                    Exchange.HTTP_RESPONSE_CODE, 201);
            })
            .log("Order saved and published to Artemis. Order ID: ${body.orderId}");

        // Retrieve order
        from("direct:getOrder")
            .routeId("get-order")
            .process(exchange -> {
                String orderId = exchange.getMessage()
                    .getHeader("orderId", String.class);

                OrderEntity order = orderService.getOrder(orderId);

                exchange.getMessage().setBody(order);
            });
    }
}
