
package com.example.orderintegration.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.orderintegration.dto.EdiPurchaseOrder;
import com.example.orderintegration.dto.OrderRequest;
import com.example.orderintegration.processor.Edi850Generator;
import com.example.orderintegration.processor.Edi850Parser;
import com.example.orderintegration.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@RestController
@RequestMapping("/api/edi/purchase-orders")
public class EdiPurchaseOrderController {

    private static final Path RECEIVED_DIR =
            Paths.get("C:/CamelProject/file-based-scenarios/edi-files/received");
    private static final Path OUTBOUND_DIR =
            Paths.get("C:/CamelProject/file-based-scenarios/edi-files/outbound");

    private final Edi850Parser edi850Parser;
    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final Validator validator;
    private final Edi850Generator edi850Generator;

    public EdiPurchaseOrderController(
            Edi850Parser edi850Parser,
            ObjectMapper objectMapper,
            OrderService orderService,
            Validator validator,
            Edi850Generator edi850Generator) {

        this.edi850Parser = edi850Parser;
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.validator = validator;
        this.edi850Generator = edi850Generator;
    }

    @PostMapping(
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receivePurchaseOrder(
            @RequestBody String ediContent) throws Exception {

        if (ediContent == null || ediContent.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("{\"message\":\"EDI content cannot be empty\"}");
        }

        Files.createDirectories(RECEIVED_DIR);
        

        String fileName =
                "purchase-order-" + UUID.randomUUID() + ".edi";

        Path filePath = RECEIVED_DIR.resolve(fileName);

        // 1. Archive the original EDI message.
        Files.writeString(
                filePath, ediContent, StandardCharsets.UTF_8);

        System.out.println(
                "EDI purchase order archived: " + filePath);

        // 2. Parse the EDI 850 message.
        EdiPurchaseOrder ediOrder = edi850Parser.parse(ediContent);

        // 3. Map EDI fields to the existing order DTO.
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderId(ediOrder.getOrderId());
        orderRequest.setCustomerName(ediOrder.getBuyerName());
        orderRequest.setProductCode(ediOrder.getProductCode());
        orderRequest.setQuantity(ediOrder.getQuantity());
        orderRequest.setUnitPrice(ediOrder.getUnitPrice());

        // 4. Validate the mapped order.
        Set<ConstraintViolation<OrderRequest>> violations =
                validator.validate(orderRequest);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            String errorJson = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "message",
                            "EDI order validation failed: " + errors));

            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson);
        }

        // 5. Save the order using the existing service and H2 database.
        orderService.createOrder(orderRequest, "EDI");

        System.out.println(
                "EDI purchase order saved to H2: "
                        + ediOrder.getOrderId());
        
     // Generate and save the outbound EDI document.
        Files.createDirectories(OUTBOUND_DIR);

        String outboundEdi = edi850Generator.generate(ediOrder);

        String outboundFileName =
                "outbound-" + ediOrder.getOrderId() + "-"
                        + UUID.randomUUID() + ".edi";

        Path outboundFile = OUTBOUND_DIR.resolve(outboundFileName);

        Files.writeString(
                outboundFile,
                outboundEdi,
                StandardCharsets.UTF_8);

        System.out.println(
                "Outbound EDI generated: " + outboundFile);

        // 6. Return the parsed purchase order as JSON.
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(ediOrder));
    }
}
