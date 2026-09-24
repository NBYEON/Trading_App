package com.nbyeon.papertrade.api;
import org.springframework.http.HttpStatus;
public final class TradingException extends RuntimeException {
    final HttpStatus status;
    final String code;
    TradingException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
}
