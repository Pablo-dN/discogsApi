package com.challenge.http;

import com.challenge.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;


@RestControllerAdvice
public class GlobalControllerExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public HttpErrorInfo handleResourceNotFoundException(ServerHttpRequest request, Exception ex) {
        return new HttpErrorInfo(
                request.getPath().toString(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidInputException.class)
    public Mono<ResponseEntity<HttpErrorInfo>> handleInvalidInputException(
            InvalidInputException ex, ServerWebExchange exchange) {

        HttpErrorInfo errorInfo = createHttpErrorInfo(HttpStatus.UNPROCESSABLE_ENTITY, exchange.getRequest().getPath().toString(), ex);
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorInfo));
    }

    @ExceptionHandler(ExternalApiException.class)
    public Mono<ResponseEntity<HttpErrorInfo>> handleExternalApiException(
            ExternalApiException ex, ServerWebExchange exchange) {

        HttpErrorInfo errorInfo = createHttpErrorInfo(HttpStatus.BAD_GATEWAY, exchange.getRequest().getPath().toString(), ex);
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorInfo));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public Mono<ResponseEntity<HttpErrorInfo>> handleTooManyRequestsException(
            TooManyRequestsException ex, ServerWebExchange exchange) {

        HttpErrorInfo errorInfo = createHttpErrorInfo(HttpStatus.TOO_MANY_REQUESTS, exchange.getRequest().getPath().toString(), ex);
        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorInfo));
    }

    private HttpErrorInfo createHttpErrorInfo(HttpStatus httpStatus, String path, Exception ex) {
        return new HttpErrorInfo(
                path,
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                ex.getMessage()
        );
    }
}
