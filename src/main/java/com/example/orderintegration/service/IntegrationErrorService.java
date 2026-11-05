package com.example.orderintegration.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.orderintegration.entity.IntegrationError;
import com.example.orderintegration.repository.IntegrationErrorRepository;

@Service
public class IntegrationErrorService {

    private final IntegrationErrorRepository repository;

    public IntegrationErrorService(IntegrationErrorRepository repository) {
        this.repository = repository;
    }

    public void recordError(
            String orderId,
            String source,
            String correlationId,
            String errorMessage) {

        IntegrationError error = new IntegrationError();
        error.setOrderId(orderId);
        error.setSource(source);
        error.setCorrelationId(correlationId);
        error.setErrorMessage(errorMessage);
        error.setErrorTime(LocalDateTime.now());

        repository.save(error);
    }
}