package com.alekss.kartveli.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.alekss.kartveli.Prefs;

public final class SecurePrefs {
    private SecurePrefs() {}

    public static String apiKey(Context context) {
        SharedPreferences secure = open(context);
        if (secure != null) {
            String value = secure.getString("api_key", "");
            if (value != null && !value.isEmpty()) return value;
        }
        return Prefs.of(context).getString(Prefs.KEY_API, "");
    }

    public static void saveApiKey(Context context, String key) {
        SharedPreferences secure = open(context);
        if (secure != null) secure.edit().putString("api_key", key).apply();
        Prefs.of(context).edit().remove(Prefs.KEY_API).apply();
    }

    private static SharedPreferences open(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            return EncryptedSharedPreferences.create(context, "kartveli_secure", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            return null;
        }
    }
}
