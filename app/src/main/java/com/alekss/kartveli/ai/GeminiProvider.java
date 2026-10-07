package com.alekss.kartveli.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GeminiProvider implements AIProvider {
    private final String apiKey;
    private final String model;

    public GeminiProvider(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model == null || model.isEmpty() ? "gemini-2.5-flash" : model;
    }

    @Override
    public Result ask(String system, JSONArray messages, String imageB64, String imageMime) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new Result(false, "ჯერ ჩაწერე Gemini API გასაღები პარამეტრებში.", "auth");
        }
        Result last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            last = once(system, messages, imageB64, imageMime);
            if (last.ok || "auth".equals(last.errorCode) || "quota".equals(last.errorCode)) return last;
        }
        return last;
    }

    private Result once(String system, JSONArray messages, String imageB64, String imageMime) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent").openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(70000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("x-goog-api-key", apiKey.trim());
            OutputStream out = conn.getOutputStream();
            out.write(body(system, messages, imageB64, imageMime).getBytes(StandardCharsets.UTF_8));
            out.close();
            int code = conn.getResponseCode();
            String raw = read(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
            if (code == 401 || code == 403) return new Result(false, "გასაღები არ ვარგებს. შეამოწმე პარამეტრებში.", "auth");
            if (code == 429) return new Result(false, "დღევანდელი ლიმიტი ამოიწურა. მოგვიანებით სცადე.", "quota");
            if (code >= 400) return new Result(false, "AI სერვისმა დროებით ვერ უპასუხა. თავიდან სცადე.", "api");
            JSONObject json = new JSONObject(raw);
            JSONArray parts = json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) text.append(parts.getJSONObject(i).optString("text"));
            if (text.length() == 0) return new Result(false, "პასუხი ცარიელია.", "empty");
            return new Result(true, text.toString().trim(), null);
        } catch (Exception e) {
            return new Result(false, "ინტერნეტთან დაკავშირება ვერ მოხერხდა.", "network");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String body(String system, JSONArray messages, String imageB64, String imageMime) throws Exception {
        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        for (int i = 0; i < messages.length(); i++) {
            JSONObject msg = messages.getJSONObject(i);
            JSONObject item = new JSONObject();
            item.put("role", "assistant".equals(msg.optString("role")) ? "model" : "user");
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", msg.optString("text")));
            if (imageB64 != null && i == messages.length() - 1) {
                JSONObject inline = new JSONObject();
                inline.put("mime_type", imageMime == null ? "image/jpeg" : imageMime);
                inline.put("data", imageB64);
                parts.put(new JSONObject().put("inline_data", inline));
            }
            item.put("parts", parts);
            contents.put(item);
        }
        body.put("contents", contents);
        body.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", system))));
        body.put("generationConfig", new JSONObject().put("temperature", 0.6));
        return body.toString();
    }

    private String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) out.append(line);
        reader.close();
        return out.toString();
    }
}
