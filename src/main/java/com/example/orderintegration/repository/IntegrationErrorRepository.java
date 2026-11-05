package com.example.orderintegration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.orderintegration.entity.IntegrationError;

public interface IntegrationErrorRepository
        extends JpaRepository<IntegrationError, Long> {
}