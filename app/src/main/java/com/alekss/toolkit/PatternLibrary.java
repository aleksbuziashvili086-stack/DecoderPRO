package com.alekss.toolkit;

import java.util.LinkedHashMap;
import java.util.Map;

public class PatternLibrary {

    public static Map<String, String[]> getPatterns() {
        Map<String, String[]> patterns = new LinkedHashMap<>();

        patterns.put("COINS", new String[]{
            "coin", "coins", "money", "gold", "cash", "credit", "credits",
            "balance", "wallet", "currency", "bank"
        });
        patterns.put("LIVES", new String[]{
            "live", "lives", "life", "heart", "hearts", "hp", "health",
            "vitality", "respawn"
        });
        patterns.put("PREMIUM", new String[]{
            "premium", "pro", "paid", "vip", "subscription", "subscriber",
            "upgrade", "unlockall", "fullversion"
        });
        patterns.put("UNLOCK", new String[]{
            "unlock", "locked", "available", "enabled", "disabled",
            "purchased", "owned", "hasaccess"
        });
        patterns.put("ADS", new String[]{
            "ads", "advert", "interstitial", "banner", "rewarded",
            "adfree", "noads", "showad", "hidead"
        });
        patterns.put("SCORE", new String[]{
            "score", "points", "highscore", "bestscore", "record", "rank"
        });
        patterns.put("GEMS", new String[]{
            "gem", "gems", "diamond", "diamonds", "crystal", "token",
            "tokens", "ticket", "tickets", "star", "stars"
        });
        patterns.put("WEAPON", new String[]{
            "weapon", "ammo", "bullet", "bullets", "gun", "guns",
            "knife", "sword", "damage", "attack"
        });
        patterns.put("POWER", new String[]{
            "power", "boost", "speed", "jump", "magnet", "shield",
            "freeze", "multiplier", "turbo"
        });
        patterns.put("GOD", new String[]{
            "god", "invincible", "immortal", "invulnerable", "infinite",
            "unlimited", "godmode", "nodamage"
        });
        patterns.put("LEVEL", new String[]{
            "level", "stage", "mission", "quest", "chapter", "progress",
            "experience", "xp"
        });
        patterns.put("PLAYER", new String[]{
            "player", "character", "hero", "avatar", "profile", "user"
        });
        patterns.put("ENERGY", new String[]{
            "energy", "stamina", "fuel", "mana", "charge", "powerup"
        });
        patterns.put("TIME", new String[]{
            "cooldown", "timer", "delay", "duration", "timescale"
        });
        patterns.put("PURCHASE", new String[]{
            "purchase", "billing", "checkout", "transaction", "receipt",
            "iap", "sku", "product"
        });
        patterns.put("BOOST", new String[]{
            "boost", "upgrade", "levelup", "enhance", "improve", "bonus"
        });
        patterns.put("ACHIEVE", new String[]{
            "achievement", "trophy", "badge", "reward"
        });
        return patterns;
    }

    public static boolean matches(String name, String keyword) {
        if (name == null || keyword == null) return false;
        return name.toLowerCase().contains(keyword.toLowerCase());
    }
}
