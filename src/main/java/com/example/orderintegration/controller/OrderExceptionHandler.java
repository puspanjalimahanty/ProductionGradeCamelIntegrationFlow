package com.example.orderintegration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.orderintegration.exception.DuplicateOrderException;
import com.example.orderintegration.service.IntegrationErrorService;

@RestControllerAdvice
public class OrderExceptionHandler {
	
	private static final Logger log =
	        LoggerFactory.getLogger(OrderExceptionHandler.class);

	private final IntegrationErrorService integrationErrorService;

	public OrderExceptionHandler(
	        IntegrationErrorService integrationErrorService) {
	    this.integrationErrorService = integrationErrorService;
	}

    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<String> handleDuplicateOrder(
            DuplicateOrderException ex) {
    	log.warn("Duplicate REST order rejected: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationError(
            MethodArgumentNotValidException ex) {
    	log.warn("REST order validation failed: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body("Invalid order request. Check the required fields and values.");
    }
}