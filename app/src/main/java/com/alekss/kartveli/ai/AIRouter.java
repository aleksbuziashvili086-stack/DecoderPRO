package com.alekss.kartveli.ai;

public final class AIRouter {
    private AIRouter() {}

    public static IntentType route(String modeId, String text, boolean hasFile, boolean hasImage) {
        if (hasImage) return IntentType.VISION;
        if (hasFile) return IntentType.FILE_ANALYSIS;
        if (modeId != null) {
            switch (modeId) {
                case "plan": return IntentType.DAY_PLAN;
                case "email": case "formal": case "resume": return IntentType.EMAIL;
                case "code": case "fix": return IntentType.CODING;
                case "study": case "explain": case "quiz": case "example": case "simple": case "exam": return IntentType.LEARNING;
                case "spell": case "rewrite": case "essay": case "analyze": return IntentType.WRITING;
                case "idea": case "product": case "client": return IntentType.BUSINESS;
                case "creative": case "post": case "poem": case "caption": case "script": return IntentType.CREATIVE;
                case "advice": case "motive": case "reflect": return IntentType.PERSONAL;
                default: break;
            }
        }
        String q = text == null ? "" : text.toLowerCase();
        if (q.contains("კოდ") || q.contains("bug") || q.contains("python") || q.contains("java")) return IntentType.CODING;
        if (q.contains("გეგმ") || q.contains("დღეს")) return IntentType.DAY_PLAN;
        if (q.contains("ბიზნეს") || q.contains("იდეა")) return IntentType.BUSINESS;
        if (q.contains("განცხად") || q.contains("ემეილ")) return IntentType.EMAIL;
        if (q.contains("რა არის") || q.contains("ამიხსენი")) return IntentType.LEARNING;
        if (q.contains("გასწორ") || q.contains("გადაწერ")) return IntentType.WRITING;
        return IntentType.GENERAL;
    }
}
