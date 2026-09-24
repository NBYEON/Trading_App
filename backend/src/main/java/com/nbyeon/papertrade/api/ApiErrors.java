package com.nbyeon.papertrade.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import static com.nbyeon.papertrade.api.Models.*;

@RestControllerAdvice
public final class ApiErrors {
    @ExceptionHandler(TradingException.class)
    ResponseEntity<ApiError> trading(TradingException error) {
        return ResponseEntity.status(error.status).body(new ApiError(error.code, error.getMessage()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalid(Exception error) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_ORDER", "Provide a request ID, symbol, side and quantity from 1 to 1,000,000."));
    }
}
