package com.council.election.configuration.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class HttpException extends RuntimeException {

    private final String message;
    private final HttpStatus httpStatus;

    public HttpException(String message, HttpStatus httpStatus) {
        this.message = String.format("{\"message\": \"%s\"}", message);
        this.httpStatus = httpStatus;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Mono<Void> setResponse(ServerWebExchange serverWebExchange) {
        return setResponse(serverWebExchange, this.message, MediaType.APPLICATION_JSON, this.httpStatus);
    }

    public static Mono<Void> setResponse(ServerWebExchange serverWebExchange, String message, MediaType mediaType, HttpStatus httpStatus) {
        DataBufferFactory bufferFactory = serverWebExchange.getResponse().bufferFactory();
        DataBuffer dataBuffer = bufferFactory.wrap(message.getBytes());
        serverWebExchange.getResponse().getHeaders().setContentType(mediaType);
        serverWebExchange.getResponse().setStatusCode(httpStatus);
        return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
    }
}
