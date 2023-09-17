package com.council.election.configuration.webflux.filter;

import com.council.election.configuration.property.Properties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

@DependsOn("properties")
@Configuration
public class Filter implements WebFilter {

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
        exchange.getResponse().getHeaders().set("server", serverName);
        exchange.getResponse().getHeaders().set("X-Powered-By", poweredBy);


        //exchange.getResponse().getHeaders().set("Authorization", exchange.getRequest().getCookies().);
/*        final String path = exchange.getRequest().getPath().value();

        exchange.getRequest().getQueryParams().toSingleValueMap().forEach((key, value) -> {
            System.out.println(key + ":" + value + "  >>>>>>> " + path);
        });

        exchange.getFormData().flatMap(
                form -> {
                    form.toSingleValueMap().forEach(
                            (key, value) -> {
                                System.out.println(key + ":" + value + "  >>>>>>> " + path);
                            }
                    );
                    return Mono.just(form);
                }
        )
                .subscribe();*/

        return webFilterChain.filter(exchange);
    }


}
