package com.council.election.configuration.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class HttpException extends RuntimeException {

    private final String message;
    private final HttpStatus httpStatus;

    public HttpException(String message, HttpStatus httpStatus) {
        Map<String, String> payload = new HashMap<>();
        payload.put("message", message);

        String json = "";
        try {
            json = new ObjectMapper().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            //throw new RuntimeException(e);
        }
        this.message = json;
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

    public static String convertStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter stackTrace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stackTrace));
        return stackTrace.toString();
    }
}
