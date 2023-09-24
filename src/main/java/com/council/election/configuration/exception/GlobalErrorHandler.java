package com.council.election.configuration.exception;

import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

@Configuration
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {

        HttpException httpException;
        if (throwable instanceof HttpException) {
            httpException = (HttpException) throwable;
        } else if (throwable instanceof DuplicateKeyException) {
            httpException = new HttpException("duplicateKey", HttpStatus.CONFLICT);
        } else {
            httpException = new HttpException(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return httpException.setResponse(serverWebExchange);
    }

}