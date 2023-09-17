package com.council.election.ddd.client.rest;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

public class SMSClient {


    private static String apiKey;
    private static String baseUrl;
    private static String sender;
    private static String serverDomainBaseUrl;
    private static Boolean pannelSmsActive;

    public static String getApiKey() {
        return apiKey;
    }

    public static void setApiKey(String apiKey) {
        SMSClient.apiKey = apiKey;
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static void setBaseUrl(String baseUrl) {
        SMSClient.baseUrl = baseUrl;
    }

    public static String getSender() {
        return sender;
    }

    public static void setSender(String sender) {
        SMSClient.sender = sender;
    }

    public static String getServerDomainBaseUrl() {
        return serverDomainBaseUrl;
    }

    public static void setServerDomainBaseUrl(String serverDomainBaseUrl) {
        SMSClient.serverDomainBaseUrl = serverDomainBaseUrl;
    }

    public static Boolean getPannelSmsActive() {
        return pannelSmsActive;
    }

    public static void setPannelSmsActive(Boolean pannelSmsActive) {
        SMSClient.pannelSmsActive = pannelSmsActive;
    }

    public static Flux<Map<String, Object>> sendSMS(String receptor, String phone, String code) {

//        String apiKey = "6D2B4C714F7842345A457232444741433532382B50534A4343574A545158472B717967384D4451313146593D";
//        String baseUrl = "https://api.kavenegar.com";
//        String sender = "1000596446";

//        String receptor = "09113394969";
//        String message = "سلام دوستان";

        if(pannelSmsActive){
            receptor = phone;
        }

        if (receptor != null && receptor.length() > 0 && !receptor.substring(0, 1).equals("0")) {
            receptor = "0" + receptor;
        }

        String message = String.format("%s/active/%s/%s", serverDomainBaseUrl, phone, code);
        return sendSMS(baseUrl, apiKey, sender, receptor, message);

/*        List<Map<String, Object>> result = sendSMS(baseUrl, apiKey, sender, receptor, message)
                .reduce(new ArrayList<Map<String, Object>>(), (list, map) -> {
                    list.add(map);
                    return list;
                })
                .block();*/

    }

    private static Flux<Map<String, Object>> sendSMS(String baseUrl, String apiKey, String sender, String receptor, String message) {

        String uri = "/v1/%s/sms/send.json";
        uri = String.format(uri, apiKey);

        String content = "جهت تغییر رمز روی لینک زیر کلیک کنید";
        message = String.format("%s<br/>%s", content, message);

        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<String, String>();
        multiValueMap.add("sender", sender);
        multiValueMap.add("receptor", receptor);
        multiValueMap.add("message", message);
        multiValueMap.add("type", "1");
        multiValueMap.add("date", "0");
        multiValueMap.add("localid", "");

        return WebClient
                .builder()
                .baseUrl(baseUrl)
                .build()
                .method(HttpMethod.resolve("POST"))
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(multiValueMap))
                .exchangeToFlux(response -> {
                    return response.bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
                });
    }

}
