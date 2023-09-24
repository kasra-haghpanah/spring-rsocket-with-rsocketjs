package com.council.election.configuration.log;

import com.council.election.configuration.exception.HttpException;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class Log {

    @JsonIgnore
    private final ObjectMapper objectMapper;

    @JsonIgnore
    private final Logger logger;

    @JsonProperty("requestId")
    private String requestId;
    @JsonProperty("executionTime")
    private Long executionTime;

    @JsonProperty("contextPath")
    private String contextPath;

    @JsonProperty("requestHeaders")
    private final Map<String, List<String>> requestHeaders = new HashMap<String, List<String>>();

    @JsonProperty("responseHeaders")
    private final Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();

    @JsonProperty("url")
    private String url;

    @JsonProperty("statusCode")
    private int statusCode;

    @JsonProperty("method")
    private String method;

    @JsonProperty("query")
    String query;

    @JsonProperty("requestBody")
    private Object requestBody;

    @JsonIgnore
    private final StringWriter requestBodyWriter = new StringWriter();
    @JsonProperty("responseBody")
    private Object responseBody;

    @JsonIgnore
    private final StringWriter responseBodyWriter = new StringWriter();

    @JsonProperty("stackTrace")
    private String stackTrace;

    @JsonProperty("remoteAddress")
    private String remoteAddress;

    @JsonProperty("cookies")
    private final Map<String, String> cookies = new HashMap<String, String>();

    @JsonProperty("forms")
    private final Map<String, Object> forms = new HashMap<String, Object>();

    @JsonProperty("queryParameters")
    private final Map<String, Object> queryParameters = new HashMap<String, Object>();

    @JsonProperty("user")
    private Map<String, Object> user;

    @JsonProperty("date")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss:SSS")
    private final LocalDateTime date = LocalDateTime.now();

    private Log(ObjectMapper objectMapper, Logger logger) {
        this.objectMapper = objectMapper;
        this.logger = logger;
    }

    public static Log create(ObjectMapper objectMapper, Logger logger) {
        return new Log(objectMapper, logger);
    }

    @JsonIgnore
    public void handleRequestAndResponseBody() {

        if (this.requestBody == null) {
            String request = this.requestBodyWriter.toString();
            this.requestBodyWriter.flush();
            JsonNode map = JsonUtil.toObject(this.objectMapper, (String) request, JsonNode.class, false);
            if (map != null) {
                this.requestBody = map;
            } else {
                this.requestBody = request;
            }
        }

        if (this.responseBody == null) {
            String response = this.responseBodyWriter.toString();
            this.responseBodyWriter.flush();
            JsonNode map = JsonUtil.toObject(this.objectMapper, (String) response, JsonNode.class, false);
            if (map != null) {
                this.responseBody = map;
            } else {
                this.responseBody = response;
            }
        }

        this.requestBody = requestBody;

    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public Log setExecutionTime(Long startTime) {
        if (startTime != null && startTime != 0) {
            this.executionTime = System.currentTimeMillis() - startTime;
        }
        return this;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }


    public Map<String, List<String>> getRequestHeaders() {
        return requestHeaders;
    }

    public Log addRequestHeader(String key, List<String> value) {
        this.requestHeaders.put(key, value);
        return this;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public Log addResponseHeader(String key, List<String> value) {
        this.responseHeaders.put(key, value);
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Log setUrl(String url) {
        this.url = url;
        return this;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Log setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public Log setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public Log setQuery(String query) {
        this.query = query;
        return this;
    }

    public Object getRequestBody() {
        return requestBody;
    }

    public Log setRequestBody(String requestBody) {
        this.requestBodyWriter.append(requestBody);
        return this;
    }

    @JsonIgnore
    public Log setRequestBody(Object[] requestBody) {
        this.requestBody = filterArgs(requestBody);
        return this;
    }

    public Object getResponseBody() {
        return responseBody;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public Log setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }

    @JsonIgnore
    public Log setStackTrace(Throwable throwable) {
        this.stackTrace = HttpException.convertStackTraceAsString(throwable);
        return this;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public Log setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    public Map<String, Object> getForms() {
        return forms;
    }

    public Log addForm(String key, Object value) {
        this.forms.put(key, value);
        return this;
    }

    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this.objectMapper, this);
    }

    @JsonIgnore
    public static Object[] filterArgs(Object[] ars) {
        List<Object> objects = new ArrayList<Object>();
        if (ars == null || ars.length == 0) {
            return null;
        }
        for (Object arg : ars) {
            try {
                Class aClass = arg.getClass();
                if (aClass.getName().indexOf("org.springframework") == -1) {
                    objects.add(arg);
                }

            } catch (Exception e) {
            }
        }
        return objects.toArray();
    }


    public void setResponseBody(String responseBody) {

        this.responseBodyWriter.append(responseBody);
    }

    public Map<String, Object> getUser() {
        return user;
    }

    public void setUser(Map<String, Object> user) {
        this.user = user;
    }

    @JsonIgnore
    public void setUser(Jwt jwt) {
        if (jwt != null) {
            this.user = jwt.getClaims();
        }
    }

    public Map<String, Object> getQueryParameters() {
        return queryParameters;
    }

    public Log addQueryParameters(String key, Object value) {
        this.queryParameters.put(key, value);
        return this;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public Log addCookie(String key, String value) {
        cookies.put(key, value);
        return this;
    }

    @JsonIgnore
    public void info() {
        handleRequestAndResponseBody();
        logger.info(this.toString());
    }

}
