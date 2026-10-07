package com.alekss.kartveli.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public final class MemoryRepository {
    private MemoryRepository() {}

    public static void add(Context context, String content, String type) {
        if (content == null || content.trim().isEmpty()) return;
        String low = content.toLowerCase();
        if (low.contains("api") || low.contains("password") || low.contains("token") || low.contains("პაროლ")) return;
        ContentValues values = new ContentValues();
        values.put("content", content.trim());
        values.put("type", type == null ? "PREFERENCE" : type);
        values.put("created_at", System.currentTimeMillis());
        AppDb.get(context).getWritableDatabase().insert("memories", null, values);
    }

    public static List<String> all(Context context) {
        List<String> out = new ArrayList<>();
        Cursor cursor = AppDb.get(context).getReadableDatabase().rawQuery("SELECT content FROM memories ORDER BY id DESC", null);
        while (cursor.moveToNext()) out.add(cursor.getString(0));
        cursor.close();
        return out;
    }

    public static String relevant(Context context, String query) {
        StringBuilder out = new StringBuilder();
        for (String fact : all(context)) {
            if (out.length() > 500) break;
            out.append(fact).append("; ");
        }
        return out.toString();
    }

    public static void clear(Context context) {
        AppDb.get(context).getWritableDatabase().delete("memories", null, null);
    }
}
