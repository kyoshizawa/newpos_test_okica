package jp.mcapps.android.multi_payment_terminal.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import java.util.Date;

public class JSON {

    private static final Gson gson = new GsonBuilder()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .serializeNulls()
            .disableHtmlEscaping()
            .registerTypeAdapter(Date.class, new CustomDateSerializer())
            .registerTypeAdapter(Date.class, new CustomDateDeserializer())
            .create();

    public static String stringify(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T parse(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}
