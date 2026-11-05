package com.example.orderintegration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.orderintegration.entity.ProcessedKafkaOrder;

public interface ProcessedKafkaOrderRepository
        extends JpaRepository<ProcessedKafkaOrder, String> {
}