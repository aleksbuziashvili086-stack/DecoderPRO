package com.alekss.kartveli;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class BackendClient {
    private BackendClient() {}

    public static AiClient.Result chat(Context context, String task, String chatId, String instruction, JSONArray messages, String attachment, String imageB64) {
        String base = Prefs.backend(context);
        if (base == null || base.trim().isEmpty()) {
            return new AiClient.Result(false, "ბექენდის მისამართი ცარიელია. ჩაწერე პარამეტრებში.");
        }
        try {
            JSONObject body = new JSONObject();
            body.put("user_id", "local");
            body.put("chat_id", chatId == null ? "phone" : chatId);
            body.put("task", task == null ? "free" : task);
            body.put("tone", Prefs.tone(context));
            body.put("instruction", instruction == null ? "" : instruction);
            body.put("messages", messages);
            body.put("attachment", attachment == null ? "" : attachment);
            body.put("pro", Prefs.pro(context));
            if (imageB64 != null) body.put("image_b64", imageB64);
            String raw = post(trim(base) + "/v1/chat", body.toString(), Prefs.token(context));
            JSONObject json = new JSONObject(raw);
            if (json.optBoolean("ok", true) && !json.optString("text").isEmpty()) {
                return new AiClient.Result(true, json.optString("text"));
            }
            String error = json.optString("error", json.optString("text", "ცარიელი პასუხი"));
            return new AiClient.Result(false, error);
        } catch (Exception e) {
            return new AiClient.Result(false, "დაფიქსირდა ტექნიკური შეცდომა. გთხოვ, თავიდან სცადე.");
        }
    }

    private static String trim(String base) {
        String value = base.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String post(String url, String json, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(70000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        OutputStream out = conn.getOutputStream();
        out.write(json.getBytes(StandardCharsets.UTF_8));
        out.close();
        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) text.append(line);
        reader.close();
        if (conn.getResponseCode() >= 400) throw new Exception("http");
        return text.toString();
    }
}
