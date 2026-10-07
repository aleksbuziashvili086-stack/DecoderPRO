package com.alekss.kartveli.ai;

import org.json.JSONArray;

public interface AIProvider {
    Result ask(String system, JSONArray messages, String imageB64, String imageMime);

    final class Result {
        public final boolean ok;
        public final String text;
        public final String errorCode;
        public Result(boolean ok, String text, String errorCode) {
            this.ok = ok;
            this.text = text;
            this.errorCode = errorCode;
        }
    }
}
