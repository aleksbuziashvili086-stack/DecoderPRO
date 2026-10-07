package com.alekss.kartveli.ai;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ContextManager {
    private ContextManager() {}

    public static JSONArray lastMessages(JSONArray all, int limit) {
        JSONArray out = new JSONArray();
        if (all == null) return out;
        int start = Math.max(0, all.length() - limit);
        for (int i = start; i < all.length(); i++) {
            JSONObject item = all.optJSONObject(i);
            if (item != null) out.put(item);
        }
        return out;
    }

    public static String summary(JSONArray all) {
        if (all == null || all.length() <= 12) return "";
        StringBuilder out = new StringBuilder();
        int end = all.length() - 8;
        for (int i = 0; i < end && out.length() < 700; i++) {
            JSONObject item = all.optJSONObject(i);
            if (item == null) continue;
            out.append(item.optString("role")).append(": ").append(item.optString("text")).append(" ");
        }
        return out.toString();
    }
}
