package com.council.election.configuration.exception;

import com.council.election.configuration.log.Log;
import com.council.election.configuration.property.Properties;
import com.council.election.configuration.webflux.filter.Filter;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

@DependsOn("properties")
@Configuration
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private ObjectMapper objectMapper;
    public final String serverName = Properties.getServerName();
    public final String poweredBy = Properties.getPoweredBy();

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {

        long startime = System.currentTimeMillis();
        Log log = Log.create(this.objectMapper, Filter.getLogger());
        exchange.getResponse().getHeaders().set("server", serverName);
        exchange.getResponse().getHeaders().set("X-Powered-By", poweredBy);
        log.setStackTrace(throwable);

        HttpException httpException;
        if (throwable instanceof HttpException) {
            httpException = (HttpException) throwable;
        } else if (throwable instanceof DuplicateKeyException) {
            httpException = new HttpException("duplicateKey", HttpStatus.CONFLICT);
        } else {
            httpException = new HttpException(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Filter.addLogFromServerWebExchange(exchange, log, startime).subscribe();
        return httpException.setResponse(exchange);
    }

}