package com.alekss.kartveli;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String NAME = "kartveli_prefs";
    public static final String KEY_API = "api_key";
    public static final String KEY_MODEL = "model";
    public static final String KEY_TONE = "tone";
    public static final String KEY_DARK = "dark";
    public static final String KEY_PRO = "pro_test";
    public static final String KEY_COUNT = "day_count";
    public static final String KEY_DAY = "day_stamp";

    private Prefs() {}

    public static SharedPreferences of(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String apiKey(Context context) {
        return of(context).getString(KEY_API, "");
    }

    public static String model(Context context) {
        String model = of(context).getString(KEY_MODEL, "gemini-2.5-flash");
        if (model == null || model.trim().isEmpty()) return "gemini-2.5-flash";
        return model.trim();
    }

    public static String tone(Context context) {
        return of(context).getString(KEY_TONE, "მეგობრული");
    }

    public static boolean dark(Context context) {
        return of(context).getBoolean(KEY_DARK, true);
    }

    public static boolean pro(Context context) {
        return of(context).getBoolean(KEY_PRO, false);
    }
}
