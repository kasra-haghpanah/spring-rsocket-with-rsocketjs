package com.council.election.configuration.log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;

public class JsonUtil {

    public static <T> T toObject(ObjectMapper mapper, String json, Class<T> clazz, boolean throwException) {
        if (isBlank(json)) {
            return null;
        }
        T object = null;
        try {
            object = mapper.findAndRegisterModules().readValue(json, clazz);
        } catch (Exception e) {
            if (throwException) {
                e.printStackTrace();
            }
        }
        return object;
    }

    public static <T> T toObject(ObjectMapper mapper, String json, TypeReference<T> reference, boolean throwException) {
        if (isBlank(json)) {
            return null;
        }
        T object = null;
        try {
            object = mapper.findAndRegisterModules().readValue(json, reference);
        } catch (Exception e) {
            if (throwException) {
                e.printStackTrace();
            }
        }
        return object;
    }

    public static <T> String toJson(ObjectMapper mapper, T value) {
        if (value == null) {
            return null;
        }
        String json = null;
        try {
            json = mapper.findAndRegisterModules().writeValueAsString(value);
        } catch (Exception e) {
            //  e.printStackTrace();
        }
        return json;
    }

    public static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().equals("");
        }
        if (value instanceof Collection) {
            return ((Collection) value).size() == 0;
        }
        if (value instanceof Object[]) {
            return ((Object[]) value).length == 0;
        }
        return false;
    }

}
