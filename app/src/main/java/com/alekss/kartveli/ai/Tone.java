package com.alekss.kartveli.ai;

public enum Tone {
    FRIENDLY, FORMAL, SHORT, TEACHER;

    public static Tone fromLabel(String label) {
        if ("ოფიციალური".equals(label)) return FORMAL;
        if ("მოკლე".equals(label)) return SHORT;
        if ("მასწავლებელი".equals(label)) return TEACHER;
        return FRIENDLY;
    }

    public String label() {
        switch (this) {
            case FORMAL: return "ოფიციალური";
            case SHORT: return "მოკლე";
            case TEACHER: return "მასწავლებელი";
            default: return "მეგობრული";
        }
    }
}
