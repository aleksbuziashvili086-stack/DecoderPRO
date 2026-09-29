package com.alekss.toolkit;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StreamingAnalyzer {

    private static final int CHUNK = 64 * 1024;          // 64 KB buffer
    private static final int HEADER_SAMPLE = 4096;        // 4 KB for encoding
    private static final int MAX_STRINGS = 2000;          // string cap
    private static final int MAX_FINDINGS = 300;
    private static final int MAX_FILES_LISTED = 100;

    public interface Progress {
        void update(String msg);
    }

    public static class Report {
        public long totalEntries = 0;
        public long files = 0;
        public long dirs = 0;
        public long totalCompressed = 0;
        public long totalUncompressed = 0;

        public TreeMap<String, Integer> extCount = new TreeMap<>();
        public TreeMap<String, Long> extSize = new TreeMap<>();

        public TreeSet<String> dexFiles = new TreeSet<>();
        public TreeSet<String> soFiles = new TreeSet<>();
        public TreeSet<String> unityFiles = new TreeSet<>();
        public TreeSet<String> unrealFiles = new TreeSet<>();
        public TreeSet<String> dbFiles = new TreeSet<>();
        public TreeSet<String> assetFiles = new TreeSet<>();
        public TreeSet<String> highEntropyFiles = new TreeSet<>();

        public int base64Hits = 0, hexHits = 0, urlHits = 0, jsonHits = 0;
        public int highEntropyCount = 0;

        public List<String> xorCandidates = new ArrayList<>();
        public List<String> interesting = new ArrayList<>();
        public List<String> dexHeaders = new ArrayList<>();
        public List<String> elfHeaders = new ArrayList<>();
        public List<String> urls = new ArrayList<>();
        public List<String> emails = new ArrayList<>();
        public List<String> apiKeys = new ArrayList<>();

        public StringBuilder report = new StringBuilder();
    }

    public static Report scan(InputStream apkStream, String fileName, Progress cb) throws Exception {
        Report r = new Report();
        ZipInputStream zis = new ZipInputStream(apkStream);
        ZipEntry e;

        byte[] buf = new byte[CHUNK];
        byte[] header = new byte[HEADER_SAMPLE];

        // running string sliding window
        StringBuilder strWindow = new StringBuilder();

        while ((e = zis.getNextEntry()) != null) {
            r.totalEntries++;

            String name = e.getName();
            String lower = name.toLowerCase();

            if (e.isDirectory()) {
                r.dirs++;
                continue;
            }
            r.files++;

            long size = e.getSize();
            long comp = e.getCompressedSize();
            if (size > 0) r.totalUncompressed += size;
            if (comp > 0) r.totalCompressed += comp;

            // extension counter
            int dot = lower.lastIndexOf('.');
            if (dot > 0) {
                String ext = lower.substring(dot);
                r.extCount.put(ext, r.extCount.getOrDefault(ext, 0) + 1);
                if (size > 0) r.extSize.put(ext, r.extSize.getOrDefault(ext, 0L) + size);
            }

            // SHA/MD5 across the entry (streaming)
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            // entropy histogram
            int[] freq = new int[256];
            long byteCount = 0;

            // header sample
            int headerLen = 0;

            // string window reset per entry
            strWindow.setLength(0);

            // chunked read
            int n;
            while ((n = zis.read(buf)) != -1) {
                sha.update(buf, 0, n);
                md5.update(buf, 0, n);
                for (int i = 0; i < n; i++) freq[buf[i] & 0xFF]++;
                byteCount += n;

                // capture header sample
                if (headerLen < HEADER_SAMPLE) {
                    int toCopy = Math.min(n, HEADER_SAMPLE - headerLen);
                    System.arraycopy(buf, 0, header, headerLen, toCopy);
                    headerLen += toCopy;
                }

                // string extraction from chunk
                if (r.files <= 2000 && strWindow.length() < 1024) {
                    for (int i = 0; i < n; i++) {
                        char c = (char)(buf[i] & 0xFF);
                        if (c >= 32 && c <= 126) {
                            strWindow.append(c);
                        } else {
                            if (strWindow.length() >= 6) {
                                String s = strWindow.toString();
                                if (r.urls.size() < 50 && s.startsWith("http")) r.urls.add(name + " → " + s);
                                else if (r.emails.size() < 30 && s.contains("@") && s.contains(".")) r.emails.add(s);
                                else if (r.apiKeys.size() < 20 && (s.contains("AIza") || s.contains("sk_live_"))) r.apiKeys.add(s);
                            }
                            strWindow.setLength(0);
                        }
                    }
                }
            }

            // entropy from histogram
            double ent = 0;
            if (byteCount > 0) {
                for (int c : freq) if (c > 0) {
                    double p = (double) c / byteCount;
                    ent -= p * (Math.log(p) / Math.log(2));
                }
            }

            // encoding detection on header sample
            if (headerLen > 0) {
                String sample = new String(header, 0, Math.min(headerLen, HEADER_SAMPLE), "UTF-8");
                String trimmed = sample.trim();

                if (trimmed.length() >= 16 && trimmed.length() % 4 == 0 && trimmed.matches("^[A-Za-z0-9+/\\s]+={0,2}$"))
                    r.base64Hits++;
                else if (trimmed.length() >= 16 && trimmed.replaceAll("\\s+", "").matches("^[0-9A-Fa-f]+$"))
                    r.hexHits++;
                else if (sample.contains("%") && sample.matches(".*(%[0-9A-Fa-f]{2}){3,}.*"))
                    r.urlHits++;
                else if (trimmed.startsWith("{") && trimmed.endsWith("}"))
                    r.jsonHits++;
            }

            // categorise
            if (lower.endsWith(".dex")) {
                r.dexFiles.add(name);
                if (headerLen >= 112) {
                    // DEX header
                    try {
                        int stringIds = readInt(header, 56);
                        int typeIds = readInt(header, 64);
                        int protoIds = readInt(header, 72);
                        int fieldIds = readInt(header, 80);
                        int methodIds = readInt(header, 88);
                        int classDefs = readInt(header, 96);
                        r.dexHeaders.add(String.format(
                            "%s\n   classes=%d methods=%d fields=%d strings=%d",
                            name, classDefs, methodIds, fieldIds, stringIds));
                    } catch (Exception ignored) {}
                }
            } else if (lower.endsWith(".so")) {
                r.soFiles.add(name);
                if (headerLen >= 20 && header[0]==0x7F && header[1]=='E' && header[2]=='L' && header[3]=='F') {
                    int cls = header[4] & 0xFF;
                    int data = header[5] & 0xFF;
                    String arch = readElfArch(header);
                    r.elfHeaders.add(String.format("%s [%s, %s]",
                        name, arch, cls == 2 ? "64-bit" : "32-bit"));
                }
            } else if (lower.contains("global-metadata") || lower.contains("unity") ||
                       lower.endsWith(".bundle") || lower.endsWith(".assets") ||
                       lower.endsWith(".unity3d")) {
                r.unityFiles.add(name);
            } else if (lower.endsWith(".pak")) {
                r.unrealFiles.add(name);
            } else if (lower.endsWith(".db") || lower.endsWith(".sqlite") || lower.endsWith(".sqlite3")) {
                r.dbFiles.add(name);
            } else if (lower.startsWith("assets/")) {
                if (r.assetFiles.size() < MAX_FILES_LISTED) r.assetFiles.add(name);
            }

            if (ent > 7.9) {
                r.highEntropyCount++;
                if (r.highEntropyFiles.size() < 50) {
                    r.highEntropyFiles.add(String.format("%s (%.4f)", name, ent));
                }
            }

            // interesting
            if (r.interesting.size() < MAX_FINDINGS) {
                if (ent > 7.9) r.interesting.add("[HIGH-ENTROPY] " + name);
                else if (r.base64Hits > 0 && lower.endsWith(".txt")) r.interesting.add("[B64?] " + name);
            }

            if (cb != null && r.totalEntries % 200 == 0) {
                cb.update("⏳ სკანირება: " + r.totalEntries + " entries...");
            }
        }
        zis.close();

        // build report
        r.report.append("╔══════════════════════════════════════════╗\n");
        r.report.append("║  STREAMING DEEP ANALYSIS REPORT          ║\n");
        r.report.append("║  Memory-safe • Handles 4GB+ APK          ║\n");
        r.report.append("╚══════════════════════════════════════════╝\n\n");
        r.report.append("📄 File: ").append(fileName).append("\n");
        r.report.append("📏 Compressed:   ").append(FileUtils.formatSize(r.totalCompressed)).append("\n");
        r.report.append("📏 Uncompressed: ").append(FileUtils.formatSize(r.totalUncompressed)).append("\n");
        r.report.append("📊 Entries:      ").append(r.totalEntries).append("\n");
        r.report.append("   Files:        ").append(r.files).append("\n");
        r.report.append("   Directories:  ").append(r.dirs).append("\n\n");

        r.report.append("━━━ FILE TYPES (top 30) ━━━\n");
        List<Map.Entry<String, Integer>> extList = new ArrayList<>(r.extCount.entrySet());
        extList.sort((a, b) -> b.getValue() - a.getValue());
        int shown = 0;
        for (Map.Entry<String, Integer> en : extList) {
            r.report.append(String.format("   %-15s %6d  %s\n",
                en.getKey(), en.getValue(),
                FileUtils.formatSize(r.extSize.getOrDefault(en.getKey(), 0L))));
            shown++;
            if (shown >= 30) break;
        }
        r.report.append("\n");

        r.report.append("━━━ ENCODING HITS ━━━\n");
        r.report.append("   Base64:    ").append(r.base64Hits).append("\n");
        r.report.append("   Hex:       ").append(r.hexHits).append("\n");
        r.report.append("   URL:       ").append(r.urlHits).append("\n");
        r.report.append("   JSON:      ").append(r.jsonHits).append("\n");
        r.report.append("   High-ENT:  ").append(r.highEntropyCount).append("\n\n");

        if (!r.dexHeaders.isEmpty()) {
            r.report.append("━━━ DEX FILES ━━━\n");
            for (String s : r.dexHeaders) r.report.append("   ").append(s).append("\n");
            r.report.append("\n");
        }

        if (!r.elfHeaders.isEmpty()) {
            r.report.append("━━━ ELF / .so ━━━\n");
            int c = 0;
            for (String s : r.elfHeaders) {
                r.report.append("   ").append(s).append("\n");
                if (++c >= 40) { r.report.append("   ...\n"); break; }
            }
            r.report.append("\n");
        }

        if (!r.unityFiles.isEmpty()) {
            r.report.append("━━━ UNITY FILES ━━━\n");
            int c = 0;
            for (String s : r.unityFiles) {
                r.report.append("   ").append(s).append("\n");
                if (++c >= 20) { r.report.append("   ...\n"); break; }
            }
            r.report.append("\n");
        }

        if (!r.urls.isEmpty()) {
            r.report.append("━━━ URLs FOUND ━━━\n");
            for (String s : r.urls) r.report.append("   ").append(s).append("\n");
            r.report.append("\n");
        }

        if (!r.apiKeys.isEmpty()) {
            r.report.append("━━━ API KEYS ━━━\n");
            for (String s : r.apiKeys) r.report.append("   ").append(s).append("\n");
            r.report.append("\n");
        }

        r.report.append("━━━ HIGH ENTROPY FILES ━━━\n");
        if (r.highEntropyFiles.isEmpty()) r.report.append("   (none)\n\n");
        else {
            int c = 0;
            for (String s : r.highEntropyFiles) {
                r.report.append("   ").append(s).append("\n");
                if (++c >= 30) break;
            }
            r.report.append("\n");
        }

        r.report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        r.report.append("Generated by Decoder PRO v1.0\n");
        return r;
    }

    private static int readInt(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off+1] & 0xFF) << 8) |
               ((b[off+2] & 0xFF) << 16) | ((b[off+3] & 0xFF) << 24);
    }

    private static String readElfArch(byte[] b) {
        if (b.length < 20) return "?";
        int machine = (b[18] & 0xFF) | ((b[19] & 0xFF) << 8);
        switch (machine) {
            case 0x03: return "x86";
            case 0x28: return "ARM32";
            case 0x3E: return "x86-64";
            case 0xB7: return "ARM64";
            case 0xF3: return "RISC-V";
            default: return "0x" + Integer.toHexString(machine);
        }
    }
}
