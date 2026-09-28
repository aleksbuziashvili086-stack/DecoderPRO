package com.alekss.toolkit.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alekss.toolkit.R;
import java.util.List;

public class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ViewHolder> {

    public interface OnModuleClick {
        void onClick(Module module);
    }

    public static class Module {
        public final String id;
        public final String icon;
        public final int titleRes;
        public final int descRes;

        public Module(String id, String icon, int titleRes, int descRes) {
            this.id = id;
            this.icon = icon;
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    private final Context context;
    private final List<Module> modules;
    private final OnModuleClick listener;

    public ModuleAdapter(Context context, List<Module> modules, OnModuleClick listener) {
        this.context = context;
        this.modules = modules;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_module_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Module module = modules.get(position);
        holder.icon.setText(module.icon);
        holder.title.setText(module.titleRes);
        holder.desc.setText(module.descRes);
        holder.itemView.setOnClickListener(v -> listener.onClick(module));
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView icon, title, desc;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.module_icon);
            title = itemView.findViewById(R.id.module_title);
            desc = itemView.findViewById(R.id.module_desc);
        }
    }
}
