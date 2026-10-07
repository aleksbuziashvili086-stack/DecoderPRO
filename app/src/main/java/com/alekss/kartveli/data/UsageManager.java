package com.alekss.kartveli.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class UsageManager {
    public static final int LIMIT = 20;
    private UsageManager() {}

    public static boolean allow(Context context) {
        return count(context) < LIMIT;
    }

    public static int count(Context context) {
        SQLiteDatabase db = AppDb.get(context).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT count FROM usage WHERE day = ?", new String[]{today()});
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public static void bump(Context context) {
        SQLiteDatabase db = AppDb.get(context).getWritableDatabase();
        db.execSQL("INSERT INTO usage(day, count) VALUES (?, 1) ON CONFLICT(day) DO UPDATE SET count = count + 1", new Object[]{today()});
    }

    private static String today() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }
}
