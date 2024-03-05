package com.spring.boot.spring5webapp.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Created by kasra.haghpanah on 11/13/2019.
 */
//@Configuration
public class RestTemplateConfig implements RestTemplateCustomizer , ClientHttpRequestInterceptor {


    @Override
    public void customize(RestTemplate restTemplate) {
        restTemplate.getInterceptors().add(this);
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        logRequestDetails(request);
        return execution.execute(request, body);
    }
    private void logRequestDetails(HttpRequest request) {
//        LOGGER.info("Headers: {}", request.getHeaders());
//        LOGGER.info("Request Method: {}", request.getMethod());
//        LOGGER.info("Request URI: {}", request.getURI());
    }

    //@Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }


    //@Bean
    ////@DependsOn(value = {"customRestTemplateCustomizer"})
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder(this);
    }



}
