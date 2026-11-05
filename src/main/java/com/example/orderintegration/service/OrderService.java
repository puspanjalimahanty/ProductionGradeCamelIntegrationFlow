package com.example.orderintegration.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderintegration.dto.OrderRequest;
import com.example.orderintegration.entity.OrderEntity;
import com.example.orderintegration.exception.DuplicateOrderException;
import com.example.orderintegration.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    public Optional<OrderEntity> findByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    @Transactional
    public OrderEntity createOrder(OrderRequest request, String source) {

        if (orderRepository.existsByOrderId(request.getOrderId())) {
            throw new DuplicateOrderException(request.getOrderId());
        }

        OrderEntity order = new OrderEntity();

        order.setOrderId(request.getOrderId());
        order.setCustomerName(request.getCustomerName());
        order.setProductCode(request.getProductCode());
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(request.getUnitPrice());
        order.setStatus("RECEIVED");
        order.setSource(source);

        return orderRepository.saveAndFlush(order);
    }
    

public void updateOrderStatus(String orderId, String status) {
    OrderEntity order = orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Order not found: " + orderId));

    order.setStatus(status);
    orderRepository.save(order);
}

    public boolean orderExists(String orderId) {
        return orderRepository.existsByOrderId(orderId);
    }
    
    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Order not found: " + orderId
                    )
                );
    }
}
