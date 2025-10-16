package jp.mcapps.android.multi_payment_terminal.util;

import android.annotation.SuppressLint;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomDateDeserializer implements JsonDeserializer<Date> {

    @SuppressLint("ConstantLocale")
    private static final Object[][] DATE_FORMATS = new Object[][]{
            {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()},
            {"MMM d, yyyy h:mm:ss a", Locale.ENGLISH},
            {"MMM d, yyyy hh:mm:ss", Locale.ENGLISH}
    };

    @SuppressLint("SimpleDateFormat")
    @Override
    public Date deserialize(JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context)
            throws JsonParseException {
        String dateStr = json.getAsString();
        for (Object[] format : DATE_FORMATS) {
            String formatStr = (String) format[0];
            Locale locale = (Locale) format[1];

            try {
                return new SimpleDateFormat(formatStr, locale).parse(dateStr);
            } catch (ParseException ignored) {
                // 次のフォーマットを試す
            }
        }
        throw new JsonParseException("Invalid date format: " + dateStr);
    }
}
