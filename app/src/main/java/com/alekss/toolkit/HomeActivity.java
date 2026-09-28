package com.alekss.toolkit;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.alekss.toolkit.adapters.ModuleAdapter;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        RecyclerView recyclerView = findViewById(R.id.modules_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // მოდულების სია
        List<ModuleAdapter.Module> modules = new ArrayList<>();
        modules.add(new ModuleAdapter.Module(
            "decoder", "🔐", R.string.module_decoder, R.string.module_decoder_desc));
        modules.add(new ModuleAdapter.Module(
            "hash", "#️⃣", R.string.module_hash, R.string.module_hash_desc));
        modules.add(new ModuleAdapter.Module(
            "cipher", "🔒", R.string.module_cipher, R.string.module_cipher_desc));
        modules.add(new ModuleAdapter.Module(
            "entropy", "📊", R.string.module_entropy, R.string.module_entropy_desc));
        modules.add(new ModuleAdapter.Module(
            "file_analysis", "🔬", R.string.module_file_analysis, R.string.module_file_analysis_desc));
        modules.add(new ModuleAdapter.Module(
            "hex_viewer", "🔢", R.string.module_hex_viewer, R.string.module_hex_viewer_desc));
        modules.add(new ModuleAdapter.Module(
            "strings", "🔤", R.string.module_strings, R.string.module_strings_desc));
        modules.add(new ModuleAdapter.Module(
            "apk", "📦", R.string.module_apk, R.string.module_apk_desc));
        modules.add(new ModuleAdapter.Module(
            "manifest", "📋", R.string.module_manifest, R.string.module_manifest_desc));
        modules.add(new ModuleAdapter.Module(
            "dex", "🧬", R.string.module_dex, R.string.module_dex_desc));
        modules.add(new ModuleAdapter.Module(
            "assets", "🎨", R.string.module_assets, R.string.module_assets_desc));
        modules.add(new ModuleAdapter.Module(
            "so", "⚙️", R.string.module_so, R.string.module_so_desc));

        ModuleAdapter adapter = new ModuleAdapter(this, modules, module -> {
            Toast.makeText(this, "Module: " + module.id + " (მალე!)",
                Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);
    }
}
