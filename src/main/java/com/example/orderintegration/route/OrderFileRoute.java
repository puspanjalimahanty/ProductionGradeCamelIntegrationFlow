package com.example.orderintegration.route;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.example.orderintegration.dto.OrderRequest;
import com.example.orderintegration.service.OrderService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
public class OrderFileRoute extends RouteBuilder {

    private final OrderService orderService;
    private final Validator validator;
    private final ProducerTemplate producerTemplate;

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(OrderFileRoute.class);

    public OrderFileRoute(
            OrderService orderService,
            Validator validator,
            ProducerTemplate producerTemplate) {
        this.orderService = orderService;
        this.validator = validator;
        this.producerTemplate= producerTemplate;
    }
    
    private String toOrderJson(OrderRequest request) {
        return String.format(
            "{\"orderId\":\"%s\",\"customerName\":\"%s\","
            + "\"productCode\":\"%s\",\"quantity\":%d,"
            + "\"unitPrice\":%s}",
            escapeJson(request.getOrderId()),
            escapeJson(request.getCustomerName()),
            escapeJson(request.getProductCode()),
            request.getQuantity(),
            request.getUnitPrice().toPlainString()
        );
    }

    private String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    @Override
    public void configure() {
    	
    	
    	errorHandler(
    		    deadLetterChannel("file:C:/CamelProject/file-based-scenarios/data/orders/error")
    		        .maximumRedeliveries(3)
    		        .redeliveryDelay(2000)
    		);

        from("file:C:/CamelProject/file-based-scenarios/data/orders/incoming"
                + "?include=.*\\.csv"
                + "&initialDelay=1000"
                + "&delay=5000"
                + "&readLock=changed"
                + "&move=../processed/${file:name}"
                + "&moveFailed=../error/${file:name}")
            .routeId("file-order-inbound")

            .log(LoggingLevel.INFO,
                    "Picked up order file: ${header.CamelFileName}")

            .convertBodyTo(String.class)


.process(exchange -> {

    String content = exchange.getMessage().getBody(String.class);
    String[] lines = content.split("\\R");

    if (lines.length < 2) {
        throw new IllegalArgumentException(
                "CSV file contains no order records");
    }

    String actualHeader = lines[0]
            .replace("\uFEFF", "")
            .trim();

    String expectedHeader =
            "orderId,customerName,productCode,quantity,unitPrice";

    if (!actualHeader.equalsIgnoreCase(expectedHeader)) {
        throw new IllegalArgumentException(
                "Invalid CSV header. Expected: " + expectedHeader
                + ", Received: " + actualHeader);
    }

    int processedCount = 0;
    int invalidCount = 0;

    Path errorDirectory = Path.of(
            "C:/CamelProject/file-based-scenarios/data/orders/error-records");

    Files.createDirectories(errorDirectory);

    String originalFile = exchange.getMessage()
            .getHeader("CamelFileName", String.class);

    String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));

    Path errorFile = errorDirectory.resolve(
            originalFile + "-" + timestamp + ".errors.csv");

    for (int i = 1; i < lines.length; i++) {

        String line = lines[i].trim();

        if (line.isEmpty()) {
            continue;
        }

        try {
            String[] fields = line.split(",", -1);

            if (fields.length != 5) {
                throw new IllegalArgumentException(
                        "Expected 5 fields, received " + fields.length);
            }

            OrderRequest request = new OrderRequest();
            request.setOrderId(fields[0].trim());
            request.setCustomerName(fields[1].trim());
            request.setProductCode(fields[2].trim());
            request.setQuantity(Integer.valueOf(fields[3].trim()));
            request.setUnitPrice(new BigDecimal(fields[4].trim()));

            Set<ConstraintViolation<OrderRequest>> violations =
                    validator.validate(request);

            if (!violations.isEmpty()) {
                throw new IllegalArgumentException(
                        violations.iterator().next()
                                .getPropertyPath() + ": "
                        + violations.iterator().next().getMessage());
            }

            if (orderService.orderExists(request.getOrderId())) {
                log.warn("Duplicate order skipped: {}",
                        request.getOrderId());
                continue;
            }

         // Save the order to the database
         // Save the order to the database
            orderService.createOrder(request, "FILE");

            log.info("Successfully saved order: {}",
                    request.getOrderId());

            // Convert the order to JSON
            String orderJson = toOrderJson(request);
            String correlationId = java.util.UUID.randomUUID().toString();

            producerTemplate.send("jms:queue:order.queue", ex -> {
                ex.getMessage().setBody(orderJson);
                ex.getMessage().setHeader("JMSCorrelationID", correlationId);
            });

            log.info(
                "Order message published. Order ID: {}, Correlation ID: {}",
                request.getOrderId(),
                correlationId
            );

            processedCount++;

        } catch (Exception e) {

            invalidCount++;

            String errorRecord = (i + 1) + ",\""
                    + line.replace("\"", "\"\"") + "\",\""
                    + e.getMessage().replace("\"", "\"\"") + "\""
                    + System.lineSeparator();

            Files.writeString(
                    errorFile,
                    errorRecord,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            log.error("Invalid CSV row at line {}: {}. Reason: {}",
                    i + 1, line, e.getMessage());
        }
    }

    exchange.getMessage().setHeader("processedCount", processedCount);
    exchange.getMessage().setHeader("invalidCount", invalidCount);

    log.info("File processing summary: {}, valid orders saved: {}, invalid rows: {}",
            originalFile, processedCount, invalidCount);
})
.log(LoggingLevel.INFO,
        "Finished ${header.CamelFileName}. Saved: ${header.processedCount}, invalid: ${header.invalidCount}")


            .log(LoggingLevel.INFO,
                    "Successfully processed ${header.CamelFileName}. "
                    + "Records: ${header.processedCount}");
    }
}
