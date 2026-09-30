package com.alekss.toolkit;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class PatternScanner {

    public interface Progress {
        void update(String msg);
    }

    public static class Candidate {
        public String category;
        public String className;
        public String methodName;
        public String signature;
        public String kind;
    }

    public static class Result {
        public int totalClasses = 0;
        public int totalMethods = 0;
        public int totalFields = 0;
        public Map<String, List<Candidate>> byCategory = new HashMap<>();
        public List<Candidate> all = new ArrayList<>();
        public StringBuilder report = new StringBuilder();
    }

    public static Result scan(List<File> dexFiles, Progress cb) throws Exception {
        Result r = new Result();
        Map<String, String[]> patterns = PatternLibrary.getPatterns();

        for (File dexFile : dexFiles) {
            if (cb != null) cb.update("🔬 " + dexFile.getName() + "...");
            DexFile dex = DexFileFactory.loadDexFile(dexFile, Opcodes.forApi(24));

            for (ClassDef cls : dex.getClasses()) {
                r.totalClasses++;
                String type = cls.getType();
                String className = type.startsWith("L") && type.endsWith(";")
                    ? type.substring(1, type.length() - 1).replace('/', '.')
                    : type;

                for (Method m : cls.getMethods()) {
                    r.totalMethods++;
                    String mName = m.getName();
                    for (Map.Entry<String, String[]> entry : patterns.entrySet()) {
                        String category = entry.getKey();
                        for (String keyword : entry.getValue()) {
                            if (PatternLibrary.matches(mName, keyword)) {
                                Candidate c = new Candidate();
                                c.category = category;
                                c.className = className;
                                c.methodName = mName;
                                c.kind = "method";
                                c.signature = buildSignature(m);
                                r.byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(c);
                                r.all.add(c);
                                break;
                            }
                        }
                    }
                }

                for (Field f : cls.getFields()) {
                    r.totalFields++;
                    String fName = f.getName();
                    for (Map.Entry<String, String[]> entry : patterns.entrySet()) {
                        String category = entry.getKey();
                        for (String keyword : entry.getValue()) {
                            if (PatternLibrary.matches(fName, keyword)) {
                                Candidate c = new Candidate();
                                c.category = category;
                                c.className = className;
                                c.methodName = fName;
                                c.kind = "field";
                                c.signature = shortType(f.getType());
                                r.byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(c);
                                r.all.add(c);
                                break;
                            }
                        }
                    }
                }
            }
        }

        r.report.append("╔══════════════════════════════════════╗\n");
        r.report.append("║  PATTERN SCANNER — STAGE 3           ║\n");
        r.report.append("╚══════════════════════════════════════╝\n\n");
        r.report.append("━━━ SCAN SUMMARY ━━━\n");
        r.report.append("   Classes scanned:  ").append(r.totalClasses).append("\n");
        r.report.append("   Methods scanned:  ").append(r.totalMethods).append("\n");
        r.report.append("   Fields scanned:   ").append(r.totalFields).append("\n");
        r.report.append("   Total matches:    ").append(r.all.size()).append("\n");
        r.report.append("   Categories hit:   ").append(r.byCategory.size()).append("\n\n");

        for (Map.Entry<String, List<Candidate>> entry : r.byCategory.entrySet()) {
            String category = entry.getKey();
            List<Candidate> list = entry.getValue();
            r.report.append("━━━ ").append(category)
                .append("  (").append(list.size()).append(") ━━━\n");

            int shown = 0;
            TreeSet<String> seen = new TreeSet<>();
            for (Candidate c : list) {
                String line = c.className + "::" + c.methodName + c.signature;
                if (seen.contains(line)) continue;
                seen.add(line);
                String icon = c.kind.equals("method") ? "🔧" : "📋";
                r.report.append("   ").append(icon).append(" ").append(c.className).append("\n");
                r.report.append("      → ").append(c.methodName).append(c.signature).append("\n");
                shown++;
                if (shown >= 15) {
                    r.report.append("      ... +").append(list.size() - shown).append(" more\n");
                    break;
                }
            }
            r.report.append("\n");
        }
        r.report.append("═══ READY FOR PATCHING ═══\n");
        return r;
    }

    private static String buildSignature(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        boolean first = true;
        for (CharSequence p : m.getParameterTypes()) {
            if (!first) sb.append(", ");
            sb.append(shortType(p.toString()));
            first = false;
        }
        sb.append(")");
        sb.append(shortType(m.getReturnType()));
        return sb.toString();
    }

    private static String shortType(String desc) {
        if (desc == null) return "?";
        if (desc.startsWith("L") && desc.endsWith(";"))
            return desc.substring(desc.lastIndexOf('/') + 1, desc.length() - 1);
        if (desc.startsWith("[")) return desc;
        switch (desc) {
            case "V": return "void";
            case "Z": return "boolean";
            case "B": return "byte";
            case "S": return "short";
            case "C": return "char";
            case "I": return "int";
            case "J": return "long";
            case "F": return "float";
            case "D": return "double";
        }
        return desc;
    }
}
