package com.alekss.toolkit;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

public class EntropyActivity extends Activity {

    private EditText input;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entropy);

        input = findViewById(R.id.entropy_input);
        output = findViewById(R.id.entropy_output);

        Button btnCalc = findViewById(R.id.btn_entropy);
        Button btnClear = findViewById(R.id.btn_entropy_clear);

        btnCalc.setOnClickListener(v -> calculate());
        btnClear.setOnClickListener(v -> {
            input.setText("");
            output.setText("");
        });
    }

    private void calculate() {
        String text = input.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "შეიყვანე ტექსტი!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<Character, Integer> freq = new HashMap<>();
        for (char c : text.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }

        double entropy = 0.0;
        int len = text.length();
        for (int count : freq.values()) {
            double p = (double) count / len;
            entropy -= p * (Math.log(p) / Math.log(2));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 Entropy Analysis\n");
        sb.append("─────────────────────\n");
        sb.append("Input length: ").append(len).append(" chars\n");
        sb.append("Unique chars: ").append(freq.size()).append("\n");
        sb.append("Entropy: ").append(String.format("%.4f", entropy)).append(" bits/char\n");
        sb.append("Total entropy: ").append(String.format("%.2f", entropy * len)).append(" bits\n\n");

        sb.append("📈 Interpretation:\n");
        if (entropy < 1.0) {
            sb.append("  Very low — repetitive text\n");
        } else if (entropy < 2.5) {
            sb.append("  Low — normal text\n");
        } else if (entropy < 4.0) {
            sb.append("  Medium — mixed content\n");
        } else if (entropy < 6.0) {
            sb.append("  High — possibly compressed\n");
        } else {
            sb.append("  Very high — encrypted/random\n");
        }

        sb.append("\n📋 Top characters:\n");
        freq.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(10)
            .forEach(e -> {
                char c = e.getKey();
                String display = (c == '\n') ? "\\n" : (c == ' ') ? "space" : String.valueOf(c);
                sb.append("  '").append(display).append("' : ").append(e.getValue()).append("\n");
            });

        output.setText(sb.toString());
    }
}
