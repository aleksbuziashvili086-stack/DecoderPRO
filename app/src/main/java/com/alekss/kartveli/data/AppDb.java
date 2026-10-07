package com.alekss.kartveli.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class AppDb extends SQLiteOpenHelper {
    private static AppDb instance;

    private AppDb(Context context) {
        super(context.getApplicationContext(), "kartveli.db", null, 1);
    }

    public static synchronized AppDb get(Context context) {
        if (instance == null) instance = new AppDb(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE conversations (id TEXT PRIMARY KEY, title TEXT, mode TEXT, created_at INTEGER, updated_at INTEGER, summary TEXT)");
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, conversation_id TEXT, role TEXT, content TEXT, created_at INTEGER, mode TEXT)");
        db.execSQL("CREATE TABLE memories (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT, type TEXT, created_at INTEGER)");
        db.execSQL("CREATE TABLE usage (day TEXT PRIMARY KEY, count INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
