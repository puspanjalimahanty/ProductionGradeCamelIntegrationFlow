package com.example.orderintegration.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.example.orderintegration.service.OrderService;

@Component
public class EdiSftpRoute extends RouteBuilder {

    private final OrderService orderService;

    public EdiSftpRoute(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void configure() {

        onException(Exception.class)
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .useExponentialBackOff()
            .backOffMultiplier(2.0)
            .log(LoggingLevel.ERROR,
                "SFTP delivery failed for ${header.CamelFileName}: "
                + "${exception.message}")
            .process(exchange -> {
                String fileName = exchange.getIn()
                    .getHeader(Exchange.FILE_NAME, String.class);

                String orderId = extractOrderId(fileName);

                if (orderId != null) {
                    try {
                        orderService.updateOrderStatus(
                            orderId, "EDI_FAILED");
                    } catch (IllegalArgumentException ex) {
                        log.warn(
                            "Could not update EDI_FAILED status for order {}: {}",
                            orderId, ex.getMessage());
                    }
                }
            })
            .handled(true);

        from("file:C:/CamelProject/file-based-scenarios/edi-files/outbound"
                + "?include=.*\\.edi"
                + "&move=sent/${file:name}"
                + "&readLock=changed")
            .routeId("edi-outbound-sftp")
            .log(LoggingLevel.INFO,
                "Uploading outbound EDI file: ${file:name}")
            .to("sftp://localhost:2222/outbound"
                + "?username=cameluser"
                + "&password=camel123"
                + "&autoCreate=true"
                + "&disconnect=true")
            .process(exchange -> {
                String fileName = exchange.getIn()
                    .getHeader(Exchange.FILE_NAME, String.class);

                String orderId = extractOrderId(fileName);

                if (orderId != null) {
                    try {
                        orderService.updateOrderStatus(
                            orderId, "EDI_SENT");
                    } catch (IllegalArgumentException ex) {
                        log.warn(
                            "SFTP upload succeeded, but EDI_SENT status "
                            + "could not be updated for order {}: {}",
                            orderId, ex.getMessage());
                    }
                }
            })
            .log(LoggingLevel.INFO,
                "Outbound EDI uploaded successfully: ${file:name}");
    }

    private String extractOrderId(String fileName) {
        if (fileName == null
                || !fileName.startsWith("outbound-")
                || !fileName.endsWith(".edi")) {
            return null;
        }

        String value = fileName.substring(
            "outbound-".length(),
            fileName.length() - ".edi".length());

        if (value.length() <= 37) {
            return null;
        }

        int separator = value.length() - 37;

        if (value.charAt(separator) != '-') {
            return null;
        }

        return value.substring(0, separator);
    }
}
