package com.council.election.ddd.utility;

import com.council.election.configuration.property.Properties;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.util.concurrent.ThreadLocalRandom;

public class Cookie {

    public static void setCookie(ServerWebExchange exchange, String token) {

        MultiValueMap<String, ResponseCookie> cookies = exchange.getResponse().getCookies();
        exchange.getResponse().getCookies().toSingleValueMap().forEach((key, value) -> {
            cookies.remove(key);
        });
        ResponseCookie cookie = ResponseCookie.from("Cookie", token).httpOnly(true).maxAge(Properties.getJwtExpirationTime()).build();
        exchange.getResponse().getCookies().set("Cookie", cookie);
    }

    public static String generateCode() {
        return ThreadLocalRandom.current().nextInt(10001, 99999) + "";
    }

}
