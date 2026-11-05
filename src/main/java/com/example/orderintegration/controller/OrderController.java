
package com.example.orderintegration.controller;

import java.util.UUID;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.orderintegration.dto.OrderRequest;
import com.example.orderintegration.entity.OrderEntity;
import com.example.orderintegration.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final ProducerTemplate producerTemplate;
    private final ObjectMapper objectMapper;

    public OrderController(
            OrderService orderService,
            ProducerTemplate producerTemplate,
            ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.producerTemplate = producerTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> testJson() {
        return ResponseEntity.ok("{\"message\":\"JSON response working\"}");
    }

    @GetMapping(value = "/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOrder(@PathVariable String orderId)
            throws JsonProcessingException {

        System.out.println("GET request received for order: " + orderId);

        OrderEntity order = orderService.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found: " + orderId));

        String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(order);

        System.out.println("Fetched Order JSON:\n" + json);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @PostMapping
    public ResponseEntity<String> createOrder(
            @Valid @RequestBody OrderRequest request)
            throws JsonProcessingException {

        orderService.createOrder(request, "REST");

        String correlationId = UUID.randomUUID().toString();
        String json = objectMapper.writeValueAsString(request);

        producerTemplate.send("jms:queue:order.queue", exchange -> {
            exchange.getMessage().setBody(json);
            exchange.getMessage().setHeader("JMSCorrelationID", correlationId);
        });

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order created successfully: " + request.getOrderId());
    }
}
