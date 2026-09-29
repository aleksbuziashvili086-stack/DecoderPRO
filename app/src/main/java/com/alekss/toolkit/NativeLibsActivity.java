package com.alekss.toolkit;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class NativeLibsActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_native_libs);
        TextView o = findViewById(R.id.so_output);
        o.setText("Native .so Libraries\n\nმალე დაემატება...");
    }
}
