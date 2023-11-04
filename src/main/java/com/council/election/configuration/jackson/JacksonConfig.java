package com.council.election.configuration.jackson;

import com.council.election.configuration.property.Properties;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import java.time.format.DateTimeFormatter;

@DependsOn("properties")
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    private static final String dateFormat = "yyyy-MM-dd";
    private static final String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
    private static final String localTimeFormat = "HH:mm:ss";

    @Bean
    @Primary
    @Qualifier("objectMapper")
    public ObjectMapper objectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    @DependsOn({"objectMapper"})
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization(ObjectMapper objectMapper) {
        return (builder) -> {

            builder.timeZone(Properties.getTimeZone());
            builder.configure(objectMapper);
            builder.serializers(new LocalDateSerializer(DateTimeFormatter.ofPattern(dateFormat)));
            builder.serializers(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
            builder.serializers(new LocalTimeSerializer(DateTimeFormatter.ofPattern(localTimeFormat)));
            builder.deserializers(new LocalDateDeserializer(DateTimeFormatter.ofPattern(dateFormat)));
            builder.deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
            builder.deserializers(new LocalTimeDeserializer(DateTimeFormatter.ofPattern(localTimeFormat)));
            builder.failOnUnknownProperties(false);
        };
    }

}
