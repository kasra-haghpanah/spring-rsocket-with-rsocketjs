package com.council.election.configuration.log;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.contrib.json.JsonLayoutBase;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MyJsonLayout extends JsonLayoutBase<ILoggingEvent> {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static final String TIMESTAMP_ATTR_NAME = "timestamp";
    public static final String LEVEL_ATTR_NAME = "level";
    public static final String THREAD_ATTR_NAME = "thread";
    public static final String MDC_ATTR_NAME = "mdc";
    public static final String LOGGER_ATTR_NAME = "logger";
    public static final String FORMATTED_MESSAGE_ATTR_NAME = "message";
    public static final String MESSAGE_ATTR_NAME = "raw-message";
    public static final String EXCEPTION_ATTR_NAME = "exception";
    public static final String CONTEXT_ATTR_NAME = "context";
    private static String version;
    protected boolean includeLevel = true;
    protected boolean includeThreadName = true;
    protected boolean includeMDC = true;
    protected boolean includeLoggerName = true;
    protected boolean includeFormattedMessage = true;
    protected boolean includeMessage;
    protected boolean includeException = true;
    protected boolean includeContextName = true;
    private ThrowableHandlingConverter throwableProxyConverter = new ThrowableProxyConverter();

    public MyJsonLayout() {
    }

    public static String getVersion() {
        return version;
    }

    public static void setVersion(String version) {
        MyJsonLayout.version = version;
    }

    public void start() {
        this.throwableProxyConverter.start();
        super.start();
    }

    public void stop() {
        super.stop();
        this.throwableProxyConverter.stop();
    }

    protected Map toJsonMap(ILoggingEvent var1) {
        LinkedHashMap var2 = new LinkedHashMap();
        this.addTimestamp("@timestamp", this.includeTimestamp, var1.getTimeStamp(), var2);
        this.add("@version", this.includeLevel, version, var2);
        this.add("level", this.includeLevel, String.valueOf(var1.getLevel()), var2);
        this.add("thread", this.includeThreadName, var1.getThreadName(), var2);
        this.addMap("mdc", this.includeMDC, var1.getMDCPropertyMap(), var2);
        this.add("logger", this.includeLoggerName, var1.getLoggerName(), var2);
        this.add("message", this.includeFormattedMessage, var1.getFormattedMessage(), var2);
        this.add("raw-message", this.includeMessage, var1.getMessage(), var2);
        this.add("context", this.includeContextName, var1.getLoggerContextVO().getName(), var2);
        this.addThrowableInfo("exception", this.includeException, var1, var2);
        this.addCustomDataToJsonMap(var2, var1);
        return var2;
    }

    protected void addThrowableInfo(String var1, boolean var2, ILoggingEvent var3, Map<String, Object> var4) {
        if (var2 && var3 != null) {
            IThrowableProxy var5 = var3.getThrowableProxy();
            if (var5 != null) {
                String var6 = this.throwableProxyConverter.convert(var3);
                if (var6 != null && !var6.equals("")) {
                    var4.put(var1, var6);
                }
            }
        }

    }

    protected void addCustomDataToJsonMap(Map<String, Object> var1, ILoggingEvent var2) {
        Map<String, Object> map = JsonUtil.toObject(objectMapper, (String) var1.get("message"), new TypeReference<Map<String, Object>>() {
        }, false);
        if (map == null) {
            map = new HashMap<>();
            map.put("app", var1.get("message"));
        }
        var1.put("message", map);
    }

    public boolean isIncludeLevel() {
        return this.includeLevel;
    }

    public void setIncludeLevel(boolean var1) {
        this.includeLevel = var1;
    }

    public boolean isIncludeLoggerName() {
        return this.includeLoggerName;
    }

    public void setIncludeLoggerName(boolean var1) {
        this.includeLoggerName = var1;
    }

    public boolean isIncludeFormattedMessage() {
        return this.includeFormattedMessage;
    }

    public void setIncludeFormattedMessage(boolean var1) {
        this.includeFormattedMessage = var1;
    }

    public boolean isIncludeMessage() {
        return this.includeMessage;
    }

    public void setIncludeMessage(boolean var1) {
        this.includeMessage = var1;
    }

    public boolean isIncludeMDC() {
        return this.includeMDC;
    }

    public void setIncludeMDC(boolean var1) {
        this.includeMDC = var1;
    }

    public boolean isIncludeThreadName() {
        return this.includeThreadName;
    }

    public void setIncludeThreadName(boolean var1) {
        this.includeThreadName = var1;
    }

    public boolean isIncludeException() {
        return this.includeException;
    }

    public void setIncludeException(boolean var1) {
        this.includeException = var1;
    }

    public boolean isIncludeContextName() {
        return this.includeContextName;
    }

    public void setIncludeContextName(boolean var1) {
        this.includeContextName = var1;
    }

    public ThrowableHandlingConverter getThrowableProxyConverter() {
        return this.throwableProxyConverter;
    }

    public void setThrowableProxyConverter(ThrowableHandlingConverter var1) {
        this.throwableProxyConverter = var1;
    }
}
