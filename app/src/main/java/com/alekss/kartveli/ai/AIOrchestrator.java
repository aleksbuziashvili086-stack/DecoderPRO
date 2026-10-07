package com.alekss.kartveli.ai;

import android.content.Context;
import com.alekss.kartveli.Mode;
import com.alekss.kartveli.Prefs;
import com.alekss.kartveli.data.MemoryRepository;
import com.alekss.kartveli.data.SecurePrefs;
import com.alekss.kartveli.data.UsageManager;
import org.json.JSONArray;

public final class AIOrchestrator {
    private AIOrchestrator() {}

    public static AIProvider.Result send(Context context, Mode mode, String text, JSONArray history, String fileText, String imageB64, String imageMime) {
        if (!UsageManager.allow(context) && !Prefs.pro(context)) {
            return new AIProvider.Result(false, "დღევანდელი 20 ლოკალური პასუხი ამოიწურა. ეს ტელეფონის ლიმიტია, სერვერი არ აკონტროლებს.", "quota");
        }
        IntentType intent = AIRouter.route(mode == null ? null : mode.id, text, fileText != null && !fileText.isEmpty(), imageB64 != null);
        String memory = Prefs.of(context).getBoolean("memory_on", true) ? MemoryRepository.relevant(context, text) : "";
        String system = PromptEngine.build(Tone.fromLabel(Prefs.tone(context)), intent, mode, memory, ContextManager.summary(history));
        JSONArray messages = ContextManager.lastMessages(history, 8);
        AIProvider provider = new GeminiProvider(SecurePrefs.apiKey(context), Prefs.model(context));
        AIProvider.Result result = provider.ask(system, messages, imageB64, imageMime);
        if (result.ok) UsageManager.bump(context);
        return result;
    }
}
