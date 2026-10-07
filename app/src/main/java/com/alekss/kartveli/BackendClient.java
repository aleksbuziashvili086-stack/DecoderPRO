package com.alekss.kartveli;

import android.content.Context;
import android.util.Base64;
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

    public static AiClient.Result chat(Context context, String task, JSONArray messages, String attachment, byte[] image) {
        String base = Prefs.backend(context);
        if (base == null || base.isEmpty()) {
            return AiClient.ask(Prefs.apiKey(context), Prefs.model(context), PromptLibrary.system(Prefs.tone(context), Mode.find(task)), messages);
        }
        try {
            JSONObject body = new JSONObject();
            body.put("user_id", "local");
            body.put("chat_id", "phone");
            body.put("task", task);
            body.put("tone", Prefs.tone(context));
            body.put("messages", messages);
            body.put("attachment", attachment == null ? "" : attachment);
            if (image != null) body.put("image_b64", Base64.encodeToString(image, Base64.NO_WRAP));
            String raw = post(base + "/v1/chat", body.toString(), Prefs.token(context));
            JSONObject json = new JSONObject(raw);
            return new AiClient.Result(true, json.optString("text", "ცარიელი პასუხი"));
        } catch (Exception e) {
            return new AiClient.Result(false, "ბექენდი ვერ ვიპოვე. " + e.getMessage());
        }
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
        if (conn.getResponseCode() >= 400) throw new Exception(text.toString());
        return text.toString();
    }
}
