package com.alekss.toolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringsActivity extends Activity {

    private EditText input;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strings);

        input = findViewById(R.id.strings_input);
        output = findViewById(R.id.strings_output);

        Button btnExtract = findViewById(R.id.btn_strings);
        Button btnCopy = findViewById(R.id.btn_strings_copy);
        Button btnClear = findViewById(R.id.btn_strings_clear);

        btnExtract.setOnClickListener(v -> extractStrings());
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("strings", output.getText().toString()));
            Toast.makeText(this, "✅ დაკოპირდა!", Toast.LENGTH_SHORT).show();
        });
        btnClear.setOnClickListener(v -> {
            input.setText("");
            output.setText("");
        });
    }

    private void extractStrings() {
        String text = input.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "შეიყვანე ტექსტი!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🔤 Strings Extraction\n");
        sb.append("─────────────────────\n\n");

        // 1. ASCII strings (min 4 chars)
        Pattern asciiPattern = Pattern.compile("[\\x20-\\x7E]{4,}");
        Matcher asciiMatcher = asciiPattern.matcher(text);
        int asciiCount = 0;
        sb.append("📋 ASCII Strings:\n");
        while (asciiMatcher.find()) {
            sb.append("  ").append(asciiMatcher.group()).append("\n");
            asciiCount++;
            if (asciiCount >= 50) {
                sb.append("  ... (მეტი ვერ ჩაეტია)\n");
                break;
            }
        }
        if (asciiCount == 0) sb.append("  (ვერ მოიძებნა)\n");

        // 2. URLs
        Pattern urlPattern = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
        Matcher urlMatcher = urlPattern.matcher(text);
        int urlCount = 0;
        sb.append("\n🌐 URLs:\n");
        while (urlMatcher.find()) {
            sb.append("  ").append(urlMatcher.group()).append("\n");
            urlCount++;
            if (urlCount >= 20) break;
        }
        if (urlCount == 0) sb.append("  (ვერ მოიძებნა)\n");

        // 3. Emails
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher emailMatcher = emailPattern.matcher(text);
        int emailCount = 0;
        sb.append("\n📧 Emails:\n");
        while (emailMatcher.find()) {
            sb.append("  ").append(emailMatcher.group()).append("\n");
            emailCount++;
            if (emailCount >= 20) break;
        }
        if (emailCount == 0) sb.append("  (ვერ მოიძებნა)\n");

        // 4. IP addresses
        Pattern ipPattern = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
        Matcher ipMatcher = ipPattern.matcher(text);
        int ipCount = 0;
        sb.append("\n🖥️ IP Addresses:\n");
        while (ipMatcher.find()) {
            sb.append("  ").append(ipMatcher.group()).append("\n");
            ipCount++;
            if (ipCount >= 20) break;
        }
        if (ipCount == 0) sb.append("  (ვერ მოიძებნა)\n");

        sb.append("\n─────────────────────\n");
        sb.append("სულ: ASCII=").append(asciiCount)
          .append(", URLs=").append(urlCount)
          .append(", Emails=").append(emailCount)
          .append(", IPs=").append(ipCount);

        output.setText(sb.toString());
    }
}
