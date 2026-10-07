package com.alekss.kartveli;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class AiClient {
    public static final class Result {
        public final boolean ok;
        public final String text;
        public Result(boolean ok, String text) { this.ok = ok; this.text = text; }
    }
    private AiClient() {}

    public static Result ask(String apiKey, String model, String system, JSONArray messages) {
        return ask(apiKey, model, system, messages, null, null);
    }

    public static Result ask(String apiKey, String model, String system, JSONArray messages, String imageB64, String imageMime) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new Result(false, "ჯერ ჩაწერე Gemini API გასაღები პარამეტრებში. აიღე უფასოდ aistudio.google.com-ზე.");
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent").openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(70000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("x-goog-api-key", apiKey.trim());
            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            for (int i = 0; i < messages.length(); i++) {
                JSONObject msg = messages.getJSONObject(i);
                JSONObject item = new JSONObject();
                item.put("role", "assistant".equals(msg.optString("role")) ? "model" : "user");
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", msg.optString("text"));
                parts.put(part);
                if (imageB64 != null && i == messages.length() - 1) {
                    JSONObject image = new JSONObject();
                    JSONObject inline = new JSONObject();
                    inline.put("mime_type", imageMime == null ? "image/jpeg" : imageMime);
                    inline.put("data", imageB64);
                    image.put("inline_data", inline);
                    parts.put(image);
                }
                item.put("parts", parts);
                contents.put(item);
            }
            body.put("contents", contents);
            JSONObject sys = new JSONObject();
            JSONArray sysParts = new JSONArray();
            JSONObject sysText = new JSONObject();
            sysText.put("text", system);
            sysParts.put(sysText);
            sys.put("parts", sysParts);
            body.put("systemInstruction", sys);
            JSONObject gen = new JSONObject();
            gen.put("temperature", 0.7);
            body.put("generationConfig", gen);
            OutputStream out = conn.getOutputStream();
            out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            out.close();
            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String raw = read(stream);
            if (code >= 400) return new Result(false, "სერვერმა უარი თქვა (" + code + "). შეამოწმე გასაღები და მოდელი.");
            JSONObject json = new JSONObject(raw);
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return new Result(false, "პასუხი ცარიელი დაბრუნდა.");
            JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
            if (content == null) return new Result(false, "პასუხის ფორმატი მოულოდნელია.");
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null) return new Result(false, "პასუხი ცარიელია.");
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) text.append(parts.getJSONObject(i).optString("text"));
            return text.length() == 0 ? new Result(false, "პასუხი ცარიელია.") : new Result(true, text.toString().trim());
        } catch (Exception e) {
            return new Result(false, "დაფიქსირდა ტექნიკური შეცდომა. გთხოვ, თავიდან სცადე.");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) out.append(line);
        reader.close();
        return out.toString();
    }
}
