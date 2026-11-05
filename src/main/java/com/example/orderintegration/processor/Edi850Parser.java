package com.example.orderintegration.processor;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.orderintegration.dto.EdiPurchaseOrder;

@Component
public class Edi850Parser {

    public EdiPurchaseOrder parse(String ediContent) {

        if (ediContent == null || ediContent.isBlank()) {
            throw new IllegalArgumentException("EDI content is empty");
        }

        EdiPurchaseOrder order = new EdiPurchaseOrder();
        String[] segments = ediContent.trim().split("~");

        for (String rawSegment : segments) {
            String segment = rawSegment.trim();

            if (segment.isEmpty()) {
                continue;
            }

            String[] fields = segment.split("\\*", -1);

            switch (fields[0]) {
                case "BEG":
                    if (fields.length > 3) {
                        order.setOrderId(fields[3].trim());
                    }
                    break;

                case "N1":
                    if (fields.length > 2 && "BY".equals(fields[1])) {
                        order.setBuyerName(fields[2].trim());
                    }
                    break;

                case "PO1":
                    if (fields.length > 4) {
                        order.setQuantity(Integer.valueOf(fields[2].trim()));
                        order.setUnitPrice(new BigDecimal(fields[4].trim()));

                        if (fields.length > 7 && "VP".equals(fields[6])) {
                            order.setProductCode(fields[7].trim());
                        } else if (fields.length > 6) {
                            order.setProductCode(fields[6].trim());
                        }
                    }
                    break;

                default:
                    // Other X12 segments are not mapped in this first version.
                    break;
            }
        }

        if (order.getOrderId() == null
                || order.getBuyerName() == null
                || order.getProductCode() == null
                || order.getQuantity() == null
                || order.getUnitPrice() == null) {
            throw new IllegalArgumentException(
                    "EDI 850 is missing required purchase-order fields");
        }

        return order;
    }
}
