package com.council.election.configuration.webflux.filter;

import com.council.election.configuration.log.Log;
import com.council.election.configuration.property.Properties;
import com.council.election.configuration.webflux.security.config.JwtConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

// https://piotrminkowski.com/2019/10/15/reactive-logging-with-spring-webflux-and-logstash/
@DependsOn("jacksonConfig")
@Configuration
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
                    var traceId = "";
                })
                .doOnError(throwable -> {
                    log.setStackTrace(throwable);
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
                                        return exchange.getResponse().getHeaders();
                                    }
                            )
                            .flatMap(httpHeaders -> {

                                httpHeaders.forEach((key, values) -> {
                                    log.addHeader(key, values);
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
                                String authorization = exchange.getRequest().getHeaders().get("Authorization").get(0);
                                if (authorization == null) {
                                    authorization = "";
                                }
                                int indexSpace = authorization.indexOf(" ");
                                if (indexSpace > 0) {
                                    authorization = authorization.substring(indexSpace + 1);
                                }
                                return JwtConfig.decoder(authorization);
                            })
                            .flatMap(jwt -> {
                                log.setUser(jwt);
                                log.setExecutionTime(startTime);
                                log.info();
                                return Mono.empty();
                            })
                            .subscribe();


                });
    }


}
