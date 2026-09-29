package com.alekss.toolkit;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class DexActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_dex);
        TextView o = findViewById(R.id.dex_output);
        o.setText("DEX Header Analyzer\n\nმალე დაემატება...");
    }
}
