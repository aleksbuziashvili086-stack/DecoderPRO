package com.alekss.toolkit;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
public class HomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_home);
        LinearLayout c = findViewById(R.id.modules_container);
        String[][] m = {
            {"🔐","Decoder","HEX / Base64 / URL","decoder"},
            {"#️⃣","Hash Lab","MD5 / SHA-256","hash"},
            {"🔒","Cipher","XOR / Caesar / ROT13","cipher"},
            {"🔓","Crypto Tools","Brute-force / Auto-detect","crypto"},
            {"📊","Entropy","Entropy analysis","entropy"},
            {"🔬","File Analysis","Deep analysis","file_analysis"},
            {"🔍","File Compare","Compare two files","file_compare"},
            {"🔢","HEX Viewer","HEX viewer","hex_viewer"},
            {"🔤","Strings","String extraction","strings"},
            {"📦","APK Explorer","APK / ZIP explorer","apk"},
            {"📋","Manifest","AndroidManifest","manifest"},
            {"🧬","DEX Header","DEX analysis","dex"},
            {"🎨","Assets","Assets explorer","assets"},
            {"⚙️","Native .so","Native libraries","so"},
            {"🎮","Game Lab","Game analysis","game_lab"},
            {"🧬","JADX Decompiler","DEX → Java source","jadx"}
        };
        for (String[] x : m) addCard(c, x[0], x[1], x[2], x[3]);
    }
    private void addCard(LinearLayout c, String icon, String title, String desc, String id) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30,30,30,30);
        card.setBackgroundColor(0xFF131824);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(20,15,20,15);
        card.setLayoutParams(p);
        TextView i = new TextView(this); i.setText(icon); i.setTextSize(28); card.addView(i);
        TextView t = new TextView(this); t.setText(title); t.setTextColor(0xFF00E5FF); t.setTextSize(16); t.setPadding(0,15,0,0); card.addView(t);
        TextView d = new TextView(this); d.setText(desc); d.setTextColor(0xFF8892B0); d.setTextSize(12); card.addView(d);
        card.setOnClickListener(v -> open(id));
        c.addView(card);
    }
    private void open(String id) {
        try {
            Class<?> t = null;
            switch (id) {
                case "decoder": t = DecoderActivity.class; break;
                case "hash": t = HashActivity.class; break;
                case "cipher": t = CipherActivity.class; break;
                case "crypto": t = CryptoActivity.class; break;
                case "entropy": t = EntropyActivity.class; break;
                case "file_analysis": t = FileAnalysisActivity.class; break;
                case "file_compare": t = FileCompareActivity.class; break;
                case "hex_viewer": t = HexViewerActivity.class; break;
                case "strings": t = StringsActivity.class; break;
                case "apk": t = ApkExplorerActivity.class; break;
                case "manifest": t = ManifestActivity.class; break;
                case "dex": t = DexActivity.class; break;
                case "assets": t = AssetsActivity.class; break;
                case "so": t = NativeLibsActivity.class; break;
                case "game_lab": t = GameLabActivity.class; break;
                case "jadx": t = DexDecompilerActivity.class; break;
            }
            if (t != null) startActivity(new Intent(this, t));
            else Toast.makeText(this, id + " (მალე!)", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "შეცდომა: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
