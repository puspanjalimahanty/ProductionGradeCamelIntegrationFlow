
package com.example.orderintegration.processor;

import org.springframework.stereotype.Component;

import com.example.orderintegration.dto.EdiPurchaseOrder;

@Component
public class Edi850Generator {

    public String generate(EdiPurchaseOrder order) {

        StringBuilder edi = new StringBuilder();

        edi.append("ST*850*0001~\n");
        edi.append("BEG*00*NE*")
           .append(order.getOrderId())
           .append("~\n");

        edi.append("N1*BY*")
           .append(order.getBuyerName())
           .append("~\n");

        edi.append("PO1*1*")
           .append(order.getQuantity())
           .append("*EA*")
           .append(order.getUnitPrice())
           .append("**VP*")
           .append(order.getProductCode())
           .append("~\n");

        edi.append("CTT*1~\n");
        edi.append("SE*5*0001~\n");

        return edi.toString();
    }
}
