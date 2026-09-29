package com.alekss.toolkit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecryptionEngine {

    public static class Result {
        public double entropy = 0;
        public String entropyVerdict = "";
        public List<String> detections = new ArrayList<>();
        public List<String> decodings = new ArrayList<>();
        public List<String> xorCandidates = new ArrayList<>();
        public List<String> caesarCandidates = new ArrayList<>();
        public List<String> foundStrings = new ArrayList<>();
        public StringBuilder report = new StringBuilder();
    }

    public static Result analyze(byte[] data, String fileName) {
        Result r = new Result();
        r.report.append("🔓 AUTO-DECRYPTION ANALYSIS\n");
        r.report.append("═══════════════════════════════════════\n");
        r.report.append("📄 File: ").append(fileName).append("\n");
        r.report.append("📏 Size: ").append(FileUtils.formatSize(data.length)).append("\n\n");

        r.entropy = entropy(data);
        r.report.append("━━━ 1. ENTROPY ━━━\n");
        r.report.append(String.format("   %.4f bits/byte\n", r.entropy));
        if (r.entropy < 2.0) r.entropyVerdict = "Very low — repetitive text/data";
        else if (r.entropy < 4.0) r.entropyVerdict = "Low — normal text";
        else if (r.entropy < 6.0) r.entropyVerdict = "Medium — mixed content";
        else if (r.entropy < 7.5) r.entropyVerdict = "High — compressed (ZIP/GZIP)";
        else r.entropyVerdict = "Very high — ENCRYPTED or compressed";
        r.report.append("   → ").append(r.entropyVerdict).append("\n\n");

        r.report.append("━━━ 2. MAGIC BYTES ━━━\n");
        String magic = detectMagic(data);
        r.report.append("   ").append(magic).append("\n\n");
        if (!magic.startsWith("Unknown")) r.detections.add(magic);

        r.report.append("━━━ 3. ENCODING DETECTION ━━━\n");
        String asText = new String(data, StandardCharsets.UTF_8);
        boolean isBase64 = looksLikeBase64(asText);
        boolean isHex = looksLikeHex(asText);
        boolean isUrl = looksLikeUrl(asText);
        boolean isJson = looksLikeJson(asText);

        if (isBase64) {
            r.report.append("   ✅ Base64-like content\n");
            try {
                byte[] decoded = android.util.Base64.decode(asText.trim(), android.util.Base64.DEFAULT);
                String dec = new String(decoded, StandardCharsets.UTF_8);
                r.report.append("   Decoded (").append(decoded.length).append(" bytes):\n");
                r.report.append("   ").append(truncate(dec, 200)).append("\n\n");
                r.decodings.add("Base64: " + truncate(dec, 100));
            } catch (Exception e) { r.report.append("   ⚠️ Base64 parse failed\n\n"); }
        }
        if (isHex) {
            r.report.append("   ✅ Hex-like content\n");
            try {
                String hex = asText.replaceAll("\\s+", "");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i + 1 < hex.length(); i += 2)
                    sb.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
                r.report.append("   Decoded: ").append(truncate(sb.toString(), 200)).append("\n\n");
                r.decodings.add("Hex: " + truncate(sb.toString(), 100));
            } catch (Exception e) {}
        }
        if (isUrl) {
            r.report.append("   ✅ URL-encoded content\n");
            try {
                String dec = java.net.URLDecoder.decode(asText, "UTF-8");
                r.report.append("   Decoded: ").append(truncate(dec, 200)).append("\n\n");
            } catch (Exception e) {}
        }
        if (isJson) r.report.append("   ✅ JSON-like structure\n\n");
        if (!isBase64 && !isHex && !isUrl && !isJson) r.report.append("   (no standard encoding detected)\n\n");

        r.report.append("━━━ 4. XOR BRUTE-FORCE (256 keys) ━━━\n");
        List<int[]> xorHits = new ArrayList<>();
        for (int key = 0; key < 256; key++) {
            int printable = 0;
            int total = Math.min(data.length, 4096);
            for (int i = 0; i < total; i++) {
                char c = (char) ((data[i] ^ key) & 0xFF);
                if (c >= 32 && c <= 126) printable++;
            }
            double score = (double) printable / total;
            if (score > 0.85) xorHits.add(new int[]{key, (int)(score * 100)});
        }
        if (xorHits.isEmpty()) {
            r.report.append("   ❌ No key produces >85% printable\n\n");
        } else {
            r.report.append("   ✅ ").append(xorHits.size()).append(" candidates:\n\n");
            for (int[] k : xorHits) {
                int key = k[0], score = k[1];
                StringBuilder line = new StringBuilder();
                int limit = Math.min(data.length, 80);
                for (int i = 0; i < limit; i++) {
                    char c = (char) ((data[i] ^ key) & 0xFF);
                    line.append((c >= 32 && c <= 126) ? c : '.');
                }
                String preview = line.toString();
                r.report.append("   Key 0x").append(String.format("%02X", key))
                    .append(" (").append(key).append(")  ").append(score).append("%\n")
                    .append("      ").append(preview).append("\n\n");
                r.xorCandidates.add("Key 0x" + String.format("%02X", key) + ": " + preview);
            }
        }

        r.report.append("━━━ 5. CAESAR BRUTE-FORCE ━━━\n");
        if (data.length > 0 && isMostlyText(data)) {
            for (int shift = 1; shift < 26; shift++) {
                StringBuilder sb = new StringBuilder();
                int limit = Math.min(data.length, 100);
                for (int i = 0; i < limit; i++) {
                    char c = (char)(data[i] & 0xFF);
                    if (c >= 'A' && c <= 'Z') sb.append((char)((c - 'A' + shift + 26) % 26 + 'A'));
                    else if (c >= 'a' && c <= 'z') sb.append((char)((c - 'a' + shift + 26) % 26 + 'a'));
                    else sb.append(c);
                }
                r.caesarCandidates.add("Shift " + shift + ": " + sb.toString());
            }
            r.report.append("   ✅ 25 shifts generated\n\n");
        } else {
            r.report.append("   ⚠️ Binary data\n\n");
        }

        r.report.append("━━━ 6. STRINGS FOUND ━━━\n");
        String[] strings = extractStrings(data, 5);
        Pattern urlPat = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
        Pattern emailPat = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Pattern apiKeyPat = Pattern.compile("(AIza[0-9A-Za-z_-]{35}|sk_live_[0-9a-zA-Z]{24,}|AKIA[0-9A-Z]{16})");
        int urls = 0, emails = 0, keys = 0;
        for (String s : strings) {
            if (urlPat.matcher(s).find()) { urls++; if (urls <= 10) r.foundStrings.add("URL: " + s); }
            if (emailPat.matcher(s).find()) { emails++; if (emails <= 5) r.foundStrings.add("Email: " + s); }
            Matcher km = apiKeyPat.matcher(s);
            if (km.find()) { keys++; r.foundStrings.add("KEY: " + km.group()); }
        }
        r.report.append("   URLs:     ").append(urls).append("\n");
        r.report.append("   Emails:   ").append(emails).append("\n");
        r.report.append("   API keys: ").append(keys).append("\n\n");

        r.report.append("━━━ 7. CONCLUSIONS ━━━\n");
        if (r.entropy > 7.5 && !isBase64 && !isHex && xorHits.isEmpty()) {
            r.report.append("   ❌ Likely AES/RSA encrypted\n");
            r.report.append("   ℹ️  Key may be inside the app itself\n");
        } else if (!xorHits.isEmpty()) {
            r.report.append("   ✅ XOR candidates found — try each key\n");
        } else if (isBase64) r.report.append("   ✅ Base64 decoded successfully\n");
        else if (isHex) r.report.append("   ✅ Hex decoded successfully\n");
        else r.report.append("   ⚠️  Unknown format\n");

        return r;
    }

    private static double entropy(byte[] b) {
        if (b.length == 0) return 0;
        int[] f = new int[256];
        for (byte x : b) f[x & 0xFF]++;
        double e = 0;
        for (int c : f) if (c > 0) {
            double p = (double) c / b.length;
            e -= p * (Math.log(p) / Math.log(2));
        }
        return e;
    }

    private static String detectMagic(byte[] b) {
        if (b.length < 4) return "Unknown (too small)";
        if (b[0]==0x50 && b[1]==0x4B && b[2]==0x03 && b[3]==0x04) return "ZIP/APK (50 4B 03 04)";
        if (b[0]==0x1F && b[1]==(byte)0x8B) return "GZIP (1F 8B)";
        if (b[0]==0x42 && b[1]==0x5A && b[2]==0x68) return "BZIP2 (BZh)";
        if (b[0]==0x7F && b[1]==0x45 && b[2]==0x4C && b[3]==0x46) return "ELF binary";
        if (b[0]==0x64 && b[1]==0x65 && b[2]==0x78 && b[3]==0x0A) return "DEX bytecode";
        if (b[0]==(byte)0x89 && b[1]==0x50 && b[2]==0x4E && b[3]==0x47) return "PNG image";
        if (b[0]==(byte)0xFF && b[1]==(byte)0xD8) return "JPEG image";
        if (b[0]==0x52 && b[1]==0x61 && b[2]==0x72 && b[3]==0x21) return "RAR archive";
        return "Unknown — candidate for encryption";
    }

    private static boolean looksLikeBase64(String s) {
        String t = s.trim();
        if (t.length() < 16 || t.length() % 4 != 0) return false;
        return t.matches("^[A-Za-z0-9+/\\s]+={0,2}$");
    }

    private static boolean looksLikeHex(String s) {
        String t = s.trim().replaceAll("\\s+", "");
        if (t.length() < 16 || t.length() % 2 != 0) return false;
        return t.matches("^[0-9A-Fa-f]+$");
    }

    private static boolean looksLikeUrl(String s) {
        return s.contains("%") && s.matches(".*(%[0-9A-Fa-f]{2}){3,}.*");
    }

    private static boolean looksLikeJson(String s) {
        String t = s.trim();
        return (t.startsWith("{") && t.endsWith("}")) ||
               (t.startsWith("[") && t.endsWith("]"));
    }

    private static boolean isMostlyText(byte[] b) {
        int printable = 0;
        int total = Math.min(b.length, 1000);
        for (int i = 0; i < total; i++) {
            char c = (char)(b[i] & 0xFF);
            if ((c >= 32 && c <= 126) || c == '\n' || c == '\r' || c == '\t') printable++;
        }
        return (double) printable / total > 0.8;
    }

    private static String[] extractStrings(byte[] data, int minLen) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (byte b : data) {
            char c = (char)(b & 0xFF);
            if (c >= 32 && c <= 126) cur.append(c);
            else {
                if (cur.length() >= minLen) out.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (cur.length() >= minLen) out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        s = s.replace("\n", " ");
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
