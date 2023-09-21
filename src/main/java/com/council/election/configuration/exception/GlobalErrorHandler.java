package com.council.election.configuration.exception;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;

@Configuration
@Order(-2)

public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    //final Logger logger = Logger.getLogger(GlobalErrorHandler.class.getName());
    private ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public class HttpError {

        private String message;

        HttpError(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {

        DataBufferFactory bufferFactory = serverWebExchange.getResponse().bufferFactory();
        if (throwable instanceof Exception) {

            //System.out.println(getStackTrace(throwable));
            //logger.info(getStackTrace(throwable));
//            while (throwable.getCause() != null) {
//                throwable = throwable.getCause();
//            }

            String message = throwable.getMessage();
            HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

            if (throwable instanceof DuplicateKeyException) {
                message = "duplicateKey";
                httpStatus = HttpStatus.CONFLICT;
            }

            serverWebExchange.getResponse().setStatusCode(httpStatus);
            DataBuffer dataBuffer = null;
            try {
                dataBuffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(new HttpError(message)));
            } catch (JsonProcessingException e) {
                dataBuffer = bufferFactory.wrap("".getBytes());
            }
            serverWebExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
        }

        serverWebExchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        serverWebExchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        DataBuffer dataBuffer = bufferFactory.wrap("Unknown error".getBytes());
        return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}