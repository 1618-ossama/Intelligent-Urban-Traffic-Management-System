package com.iutms.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Shared JSON serialization/deserialization utility.
 * Used by all services to convert objects to/from JSON for Kafka messaging.
 */
public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .setPrettyPrinting()
            .create();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static Gson getGson() {
        return GSON;
    }
}
