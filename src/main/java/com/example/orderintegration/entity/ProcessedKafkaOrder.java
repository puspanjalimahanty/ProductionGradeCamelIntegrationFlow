package com.example.orderintegration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_kafka_orders")
public class ProcessedKafkaOrder {

    @Id
    private String orderId;

    private LocalDateTime processedAt;

    public ProcessedKafkaOrder() {
    }

    public ProcessedKafkaOrder(String orderId) {
        this.orderId = orderId;
        this.processedAt = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
