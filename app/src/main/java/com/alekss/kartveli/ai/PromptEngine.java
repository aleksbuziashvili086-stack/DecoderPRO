package com.alekss.kartveli.ai;

import com.alekss.kartveli.Mode;

public final class PromptEngine {
    private PromptEngine() {}

    public static String build(Tone tone, IntentType intent, Mode mode, String memory, String summary) {
        StringBuilder out = new StringBuilder();
        out.append("შენ ხარ ქართველი AI. თუ მომხმარებელი ქართულად წერს, უპასუხე ქართულად. ");
        out.append("ქართული იყოს ბუნებრივი, არა თარგმნილი. ფაქტი თუ არ იცი, თქვი ზუსტად არ ვიცი. ");
        out.append("ვარაუდი ფაქტად არ წარმოადგინო. კოდი არ თარგმნო, ახსნა ქართულად. ");
        out.append("ტონი: ").append(tone.label()).append(". რეჟიმი: ").append(intent.name()).append(". ");
        if (mode != null) out.append("დავალება: ").append(mode.title).append(". ").append(mode.subtitle).append(". ");
        if (intent == IntentType.CODING) out.append("კოდი არ შეცვალო მოთხოვნის გარეშე. ჯერ პრობლემა, მერ მიზეზი, მერ გასწორებული ვერსია. ");
        if (memory != null && !memory.isEmpty()) out.append("დამახსოვრებული ფაქტები: ").append(memory).append(". ");
        if (summary != null && !summary.isEmpty()) out.append("წინა საუბრის შეჯამება: ").append(summary).append(". ");
        return out.toString();
    }
}
