package com.example.orderintegration.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderintegration.entity.ProcessedKafkaOrder;
import com.example.orderintegration.repository.ProcessedKafkaOrderRepository;

@Service
public class ProcessedKafkaOrderService {

    private final ProcessedKafkaOrderRepository repository;

    public ProcessedKafkaOrderService(ProcessedKafkaOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean markIfNotProcessed(String orderId) {
        if (repository.existsById(orderId)) {
            return false;
        }

        repository.saveAndFlush(new ProcessedKafkaOrder(orderId));
        return true;
    }
}