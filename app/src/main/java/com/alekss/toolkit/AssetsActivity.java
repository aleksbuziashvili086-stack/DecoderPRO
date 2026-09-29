package com.alekss.toolkit;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class AssetsActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_assets);
        TextView o = findViewById(R.id.assets_output);
        o.setText("Assets Explorer\n\nმალე დაემატება...");
    }
}
