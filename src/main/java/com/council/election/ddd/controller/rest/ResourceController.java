package com.council.election.ddd.controller.rest;

import com.council.election.configuration.property.Properties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

@Controller
@Validated
public class ResourceController {

    // https://www.baeldung.com/spring-webflux-databufferlimitexception
    @RequestMapping(value = "/resource/{version}/**", method = RequestMethod.GET)
    @ResponseBody
    public Mono<Void> resource(
            @Valid @PathVariable("version") @Pattern(regexp = "(\\/)*((\\d){1,2})\\.((\\d){1,2})\\.((\\d){1,2})(.)*") String version,
            ServerWebExchange exchange
    ) {

        if (version == null || version.equals("")) {
            return Mono.empty();
        }
        String baseUrl = MessageFormat.format("http://{0}:{1}", Properties.getServerHost(), Properties.getServerPort() + "");
        String path = exchange.getRequest().getPath().value();
        try {
            path = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            version = URLDecoder.decode(version, StandardCharsets.UTF_8.toString());
            path = path.replaceAll("[//]{2,}", "/");
            version = version.replaceAll("[//]{2,}", "/");
        } catch (UnsupportedEncodingException e) {

        }
        int slashIndex = version.indexOf("/");
        String uri = "";
        if (slashIndex > -1) {
            if (slashIndex > 0) {
                uri = version.substring(version.indexOf("/"));
            } else {
                uri = version.substring(1);
                uri = path.substring(path.indexOf(uri));
                uri = uri.substring(uri.indexOf("/"));
            }
        } else {
            uri = path.substring(path.indexOf(version) + version.length());
        }

        uri = uri.replaceAll("/\\*\\*", "");

        return WebClient
                .builder()
                .baseUrl(baseUrl)
//                .codecs(codecs -> codecs
//                        .defaultCodecs()
//                        .maxInMemorySize(2000 * 1024)
//                )
                .build()
                .method(HttpMethod.valueOf("GET"))
                .uri(uri)
                .exchangeToMono(clientResponse -> {

                    exchange.getResponse().getHeaders().clear();
                    clientResponse.headers().asHttpHeaders().forEach((key, valueAsList) -> {
                        exchange.getResponse().getHeaders().addAll(key, valueAsList);
                    });
                    //exchange.getResponse().getHeaders().setCacheControl(CacheControl.maxAge(360, TimeUnit.SECONDS));
                    return exchange.getResponse().writeWith(clientResponse.bodyToFlux(DataBuffer.class));

                });

    }

}
