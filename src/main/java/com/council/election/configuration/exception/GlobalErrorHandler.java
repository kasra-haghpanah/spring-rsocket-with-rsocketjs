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

    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {

        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            return httpException.setResponse(serverWebExchange);
        }

        //System.out.println(getStackTrace(throwable));
        //logger.info(getStackTrace(throwable));
//            while (throwable.getCause() != null) {
//                throwable = throwable.getCause();
//            }

        if (throwable instanceof DuplicateKeyException) {
            HttpException httpException = new HttpException("duplicateKey", HttpStatus.CONFLICT);
            return httpException.setResponse(serverWebExchange);
        }

        HttpException httpException = new HttpException(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        return httpException.setResponse(serverWebExchange);

    }

    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}