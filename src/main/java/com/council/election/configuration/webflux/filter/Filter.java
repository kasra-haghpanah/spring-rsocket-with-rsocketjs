package com.council.election.configuration.webflux.filter;

import com.council.election.configuration.exception.GlobalErrorHandler;
import com.council.election.configuration.exception.HttpError;
import com.council.election.configuration.log.JsonUtil;
import com.council.election.configuration.log.Log;
import com.council.election.configuration.property.Properties;
import com.council.election.configuration.webflux.security.config.JwtConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// https://piotrminkowski.com/2019/10/15/reactive-logging-with-spring-webflux-and-logstash/
@DependsOn("jacksonConfig")
@Configuration
@Order(-1)
public class Filter implements WebFilter {

    final Logger logger = Logger.getLogger(Filter.class.getName());
    private final ObjectMapper objectMapper;

    public Filter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public final String serverName = Properties.getServerName();
    public final String poweredBy = Properties.getPoweredBy();

    String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain webFilterChain) {
        //exchange.getResponse().getHeaders().getAcceptCharset().add(Charset.forName("UTF-8"));
        //exchange.getResponse().getHeaders().set("accept-language", "fa");
        long startTime = System.currentTimeMillis();
        Log log = Log.create(this.objectMapper, logger);
        exchange.getResponse().getHeaders().set("server", serverName);
        exchange.getResponse().getHeaders().set("X-Powered-By", poweredBy);

        ServerWebExchangeDecorator exchangeDecorator = new ServerWebExchangeDecorator(exchange) {
            @Override
            public ServerHttpRequest getRequest() {
                return new RequestLoggingDecorator(exchange.getRequest(), log);
            }

            @Override
            public ServerHttpResponse getResponse() {
                return new ResponseLoggingDecorator(exchange.getResponse(), log, System.currentTimeMillis(), true);
            }
        };


        return webFilterChain.filter(exchangeDecorator)
                .doOnSuccess(aVoid -> {

                })
                .doAfterTerminate(() -> {
                    // System.out.println("doAfterTerminate");
                })
                .doOnError(throwable -> {
                    log.setStackTrace(throwable);
                    DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
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

                        exchange.getResponse().setStatusCode(httpStatus);
                        DataBuffer dataBuffer = null;
                        try {
                            dataBuffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(new HttpError(message)));
                        } catch (JsonProcessingException e) {
                            dataBuffer = bufferFactory.wrap("".getBytes());
                        }
                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        exchange.getResponse().writeWith(Mono.just(dataBuffer));
                    } else {

                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
                        DataBuffer dataBuffer = bufferFactory.wrap("Unknown error".getBytes());
                        exchange.getResponse().writeWith(Mono.just(dataBuffer));
                    }


                })
                .doFinally(signalType -> {

                    exchange.getFormData()
                            .map(
                                    form -> {
                                        form.toSingleValueMap().forEach(
                                                (key, value) -> {
                                                    log.addForm(key, value);
                                                }
                                        );
                                        //return Mono.just(exchange.getMultipartData());
                                        return exchange.getRequest().getHeaders();
                                    }
                            )
                            .flatMap(httpHeaders -> {

                                httpHeaders.forEach((key, values) -> {
                                    log.addRequestHeader(key, values);
                                });

                                exchange.getResponse().getHeaders()
                                        .forEach((key, values) -> {
                                    log.addResponseHeader(key, values);
                                });

                                exchange.getRequest().getQueryParams().toSingleValueMap().forEach((key, value) -> {
                                    log.addQueryParameters(key, value);
                                });

                                log.setStatusCode(exchange.getResponse().getStatusCode().value());
                                log.setMethod(exchange.getRequest().getMethod().name());
                                log.setUrl(exchange.getRequest().getURI().getPath());
                                log.setRequestId(exchange.getRequest().getId());
                                exchange.getRequest().getCookies().toSingleValueMap().forEach((key, value) -> {
                                    log.addCookie(key, value.getValue());
                                });
                                String authorization = "";
                                List<String> authorizationList = exchange.getRequest().getHeaders().get("Authorization");
                                if (authorizationList != null && authorizationList.size() > 0) {
                                    authorization = authorizationList.get(0);
                                } else if (log.getCookies().keySet().size() > 0) {
                                    var cookieValue = log.getCookies().get("Cookie");
                                    if (!JsonUtil.isBlank(cookieValue)) {
                                        authorization = cookieValue;
                                    }
                                }
                                int indexSpace = authorization.indexOf(" ");
                                if (indexSpace > 0) {
                                    authorization = authorization.substring(indexSpace + 1);
                                }
                                try {
                                    return JwtConfig.decoder(authorization);
                                } catch (Exception e) {
                                    return Mono.just(Void.TYPE);
                                }

                            })
                            .flatMap(jwt -> {
                                if (!jwt.equals(Void.TYPE)) {
                                    log.setUser((Jwt) jwt);
                                }
                                log.setExecutionTime(startTime);
                                log.info();
                                return Mono.empty();
                            })
                            .subscribe();


                });
    }


}
