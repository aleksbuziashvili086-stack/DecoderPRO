package com.alekss.kartveli.files;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class LocalFileReader {
    private LocalFileReader() {}

    public static String readText(InputStream in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0 && total < 16000) {
                out.write(buf, 0, n);
                total += n;
            }
            in.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "ამ ფაილს ვერ ვკითხე. TXT, JSON, XML, CSV, MD ან კოდი სცადე.";
        }
    }

    public static String readZip(InputStream in) {
        StringBuilder out = new StringBuilder();
        try {
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry entry;
            int files = 0;
            while ((entry = zip.getNextEntry()) != null && files < 30) {
                String name = entry.getName();
                if (name.contains("..") || name.startsWith("/") || entry.isDirectory()) continue;
                out.append(name).append("\n");
                String lower = name.toLowerCase();
                if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".java") || lower.endsWith(".kt") || lower.endsWith(".xml") || lower.endsWith(".json") || lower.endsWith(".gradle")) {
                    byte[] buf = new byte[1024];
                    int n;
                    int total = 0;
                    ByteArrayOutputStream text = new ByteArrayOutputStream();
                    while ((n = zip.read(buf)) > 0 && total < 2000) {
                        text.write(buf, 0, n);
                        total += n;
                    }
                    out.append(text.toString("UTF-8")).append("\n");
                }
                files++;
            }
            zip.close();
        } catch (Exception e) {
            return "ZIP ვერ გაიხსნა.";
        }
        return out.length() == 0 ? "ZIP ცარიელია." : out.toString();
    }
}
