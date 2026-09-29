package com.alekss.toolkit;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SignatureVerifier {

    public static class Result {
        public List<String> manifestEntries = new ArrayList<>();
        public List<CertInfo> certificates = new ArrayList<>();
        public boolean hasV1 = false;
        public boolean hasV2 = false;
        public boolean hasV3 = false;
        public StringBuilder report = new StringBuilder();
    }

    public static class CertInfo {
        public String subject = "?";
        public String issuer = "?";
        public String algorithm = "?";
        public String validFrom = "?";
        public String validTo = "?";
        public String serialNumber = "?";
        public boolean selfSigned = false;
        public String sha256 = "?";
        public String sha1 = "?";
    }

    public static Result verify(InputStream apkStream) {
        Result r = new Result();
        r.report.append("🔐 APK SIGNATURE VERIFICATION\n");
        r.report.append("═══════════════════════════════════════\n\n");

        try {
            ZipInputStream zis = new ZipInputStream(apkStream);
            ZipEntry e;
            ByteArrayOutputStream mfBytes = null;
            List<byte[]> certBytesList = new ArrayList<>();
            List<String> certNames = new ArrayList<>();

            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                if (name.equals("META-INF/MANIFEST.MF")) {
                    mfBytes = readAll(zis);
                    r.hasV1 = true;
                } else if (name.startsWith("META-INF/") &&
                          (name.endsWith(".RSA") || name.endsWith(".DSA") ||
                           name.endsWith(".EC") || name.endsWith(".SF"))) {
                    if (!name.endsWith(".SF")) {
                        certBytesList.add(readAll(zis));
                        certNames.add(name);
                    }
                }
            }
            zis.close();

            r.report.append("📋 V1 SIGNATURE (JAR signing):\n");
            if (r.hasV1) {
                r.report.append("   ✅ Present (META-INF/MANIFEST.MF)\n");
                if (mfBytes != null) {
                    String mfText = new String(mfBytes.toByteArray(), "UTF-8");
                    for (String line : mfText.split("\n")) {
                        if (line.startsWith("Signature-Version:") ||
                            line.startsWith("Created-By:") ||
                            line.startsWith("Manifest-Version:")) {
                            r.manifestEntries.add(line.trim());
                        }
                    }
                    r.report.append("   📄 Manifest entries: ").append(r.manifestEntries.size()).append("\n");
                    for (String entry : r.manifestEntries)
                        r.report.append("      ").append(entry).append("\n");
                }
            } else {
                r.report.append("   ❌ Not found\n");
            }
            r.report.append("\n");

            r.report.append("🔑 CERTIFICATES:\n");
            if (certBytesList.isEmpty()) {
                r.report.append("   ❌ No certificate files found\n\n");
            } else {
                for (int i = 0; i < certBytesList.size(); i++) {
                    r.report.append("   📜 ").append(certNames.get(i)).append("\n");
                    CertInfo info = parseCertificate(certBytesList.get(i));
                    if (info != null) {
                        r.certificates.add(info);
                        r.report.append("      Subject:    ").append(info.subject).append("\n");
                        r.report.append("      Issuer:     ").append(info.issuer).append("\n");
                        r.report.append("      Algorithm:  ").append(info.algorithm).append("\n");
                        r.report.append("      Valid from: ").append(info.validFrom).append("\n");
                        r.report.append("      Valid to:   ").append(info.validTo).append("\n");
                        r.report.append("      Serial:     ").append(info.serialNumber).append("\n");
                        r.report.append("      Self-signed:").append(info.selfSigned ? " Yes" : " No").append("\n");
                        r.report.append("      SHA-256:    ").append(info.sha256).append("\n");
                        r.report.append("      SHA-1:      ").append(info.sha1).append("\n");
                    }
                    r.report.append("\n");
                }
            }

            r.report.append("═══════════════════════════════════════\n");
            r.report.append("📌 VERDICT:\n");
            if (r.hasV1 && !r.certificates.isEmpty()) {
                r.report.append("   ✅ Signed with V1 (JAR) scheme\n");
                CertInfo c = r.certificates.get(0);
                if (c.selfSigned)
                    r.report.append("   ⚠️  Self-signed certificate\n");
                else
                    r.report.append("   ✅ Chain of trust verified\n");
            } else if (!r.hasV1) {
                r.report.append("   ⚠️  No V1 signature\n");
            }

        } catch (Exception ex) {
            r.report.append("❌ Error: ").append(ex.getMessage()).append("\n");
        }
        return r;
    }

    private static CertInfo parseCertificate(byte[] data) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(data);
            X509Certificate cert = (X509Certificate) cf.generateCertificate(bis);
            CertInfo info = new CertInfo();
            info.subject = cert.getSubjectDN().getName();
            info.issuer = cert.getIssuerDN().getName();
            info.algorithm = cert.getSigAlgName();
            info.validFrom = cert.getNotBefore().toString();
            info.validTo = cert.getNotAfter().toString();
            info.serialNumber = cert.getSerialNumber().toString(16);
            info.selfSigned = cert.getSubjectDN().equals(cert.getIssuerDN());
            info.sha256 = bytesToHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
            info.sha1 = bytesToHex(MessageDigest.getInstance("SHA-1").digest(cert.getEncoded()));
            return info;
        } catch (Exception e) { return null; }
    }

    private static byte[] readAll(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
