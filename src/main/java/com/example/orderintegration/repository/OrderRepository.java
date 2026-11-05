package com.example.orderintegration.repository;

import com.example.orderintegration.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<OrderEntity, Long> {

    boolean existsByOrderId(String orderId);

    Optional<OrderEntity> findByOrderId(String orderId);
}
