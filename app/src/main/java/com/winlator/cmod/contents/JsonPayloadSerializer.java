package com.winlator.cmod.contents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

final class JsonPayloadSerializer {
    private JsonPayloadSerializer() {
    }

    static String toJson(JSONArray array) {
        return toJsonArray(array).toString();
    }

    private static JsonArray toJsonArray(JSONArray array) {
        JsonArray out = new JsonArray();
        if (array == null) return out;
        for (int i = 0; i < array.length(); i++) {
            out.add(toJsonElement(array.opt(i)));
        }
        return out;
    }

    private static JsonObject toJsonObject(JSONObject object) {
        JsonObject out = new JsonObject();
        if (object == null) return out;
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            out.add(key, toJsonElement(object.opt(key)));
        }
        return out;
    }

    private static JsonElement toJsonElement(Object value) {
        if (value == null || value == JSONObject.NULL) return JsonNull.INSTANCE;
        if (value instanceof JSONObject) return toJsonObject((JSONObject) value);
        if (value instanceof JSONArray) return toJsonArray((JSONArray) value);
        if (value instanceof Number) return new JsonPrimitive((Number) value);
        if (value instanceof Boolean) return new JsonPrimitive((Boolean) value);
        if (value instanceof Character) return new JsonPrimitive((Character) value);
        return new JsonPrimitive(String.valueOf(value));
    }
}
