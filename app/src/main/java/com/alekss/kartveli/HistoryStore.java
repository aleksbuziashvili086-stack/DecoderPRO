package com.alekss.kartveli;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HistoryStore {
    public static final class Chat {
        public String id;
        public String title;
        public String mode;
        public long updated;
        public JSONArray messages = new JSONArray();
    }

    private HistoryStore() {}

    private static File file(Context context) {
        return new File(context.getFilesDir(), "history.json");
    }

    public static List<Chat> list(Context context) {
        List<Chat> out = new ArrayList<>();
        JSONObject root = read(context);
        JSONArray chats = root.optJSONArray("chats");
        if (chats == null) return out;
        for (int i = 0; i < chats.length(); i++) {
            JSONObject item = chats.optJSONObject(i);
            if (item == null) continue;
            Chat chat = new Chat();
            chat.id = item.optString("id");
            chat.title = item.optString("title", "საუბარი");
            chat.mode = item.optString("mode", "free");
            chat.updated = item.optLong("updated");
            chat.messages = item.optJSONArray("messages");
            if (chat.messages == null) chat.messages = new JSONArray();
            out.add(chat);
        }
        return out;
    }

    public static Chat open(Context context, String id) {
        for (Chat chat : list(context)) {
            if (chat.id.equals(id)) return chat;
        }
        return null;
    }

    public static Chat create(Context context, String mode, String title) {
        Chat chat = new Chat();
        chat.id = UUID.randomUUID().toString();
        chat.mode = mode;
        chat.title = title == null || title.trim().isEmpty() ? "საუბარი" : title.trim();
        chat.updated = System.currentTimeMillis();
        save(context, chat);
        return chat;
    }

    public static void save(Context context, Chat chat) {
        chat.updated = System.currentTimeMillis();
        JSONObject root = read(context);
        JSONArray chats = root.optJSONArray("chats");
        if (chats == null) chats = new JSONArray();
        JSONArray next = new JSONArray();
        next.put(toJson(chat));
        for (int i = 0; i < chats.length(); i++) {
            JSONObject item = chats.optJSONObject(i);
            if (item == null) continue;
            if (chat.id.equals(item.optString("id"))) continue;
            next.put(item);
            if (next.length() >= 40) break;
        }
        try {
            JSONObject out = new JSONObject();
            out.put("chats", next);
            write(context, out.toString());
        } catch (Exception ignored) {
        }
    }

    public static void clear(Context context) {
        write(context, "{\"chats\":[]}");
    }

    private static JSONObject toJson(Chat chat) {
        JSONObject item = new JSONObject();
        try {
            item.put("id", chat.id);
            item.put("title", chat.title);
            item.put("mode", chat.mode);
            item.put("updated", chat.updated);
            item.put("messages", chat.messages);
        } catch (Exception ignored) {
        }
        return item;
    }

    private static JSONObject read(Context context) {
        try {
            File file = file(context);
            if (!file.exists()) return new JSONObject("{\"chats\":[]}");
            FileInputStream in = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            int read = in.read(data);
            in.close();
            if (read <= 0) return new JSONObject("{\"chats\":[]}");
            return new JSONObject(new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            try {
                return new JSONObject("{\"chats\":[]}");
            } catch (Exception ignored) {
                return new JSONObject();
            }
        }
    }

    private static void write(Context context, String json) {
        try {
            FileOutputStream out = new FileOutputStream(file(context), false);
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (Exception ignored) {
        }
    }
}
