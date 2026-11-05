package com.example.orderintegration.exception;

public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String orderId) {
        super("Order already exists: " + orderId);
    }
}
