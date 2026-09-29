package com.alekss.toolkit;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class FileUtils {

    public static byte[] readFileBytes(Context ctx, Uri uri) throws Exception {
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        if (is == null) throw new Exception("Cannot open file");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = is.read(buffer)) != -1) {
            baos.write(buffer, 0, n);
        }
        is.close();
        return baos.toByteArray();
    }

    public static String readFileText(Context ctx, Uri uri) throws Exception {
        byte[] bytes = readFileBytes(ctx, uri);
        return new String(bytes, "UTF-8");
    }

    public static String getFileName(Context ctx, Uri uri) {
        String name = "unknown";
        try {
            Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
                cursor.close();
            }
        } catch (Exception ignored) {}
        return name;
    }

    public static long getFileSize(Context ctx, Uri uri) {
        long size = 0;
        try {
            Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) size = cursor.getLong(idx);
                cursor.close();
            }
        } catch (Exception ignored) {}
        return size;
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
