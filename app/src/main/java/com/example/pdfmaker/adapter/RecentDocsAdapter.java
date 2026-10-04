package com.example.pdfmaker.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.R;
import com.example.pdfmaker.model.RecentDocItem;

import java.util.List;

public class RecentDocsAdapter extends RecyclerView.Adapter<RecentDocsAdapter.ViewHolder> {

    public interface OnDocActionListener {
        void onOpen(RecentDocItem item);
        void onShare(RecentDocItem item);
        void onDelete(RecentDocItem item);
        void onItemClick(RecentDocItem item);
    }

    private final Context context;
    private final List<RecentDocItem> items;
    private final OnDocActionListener listener;

    public RecentDocsAdapter(Context context, List<RecentDocItem> items, OnDocActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentDocItem item = items.get(position);
        holder.tvName.setText(item.getName());

        String meta;
        if (item.getPageCount() > 0) {
            meta = item.getFormattedDate() + " • " + item.getPageCount() + (item.getPageCount() == 1 ? " page" : " pages") + " • " + item.getFormattedSize();
        } else {
            meta = item.getFormattedDate() + " • " + item.getFormattedSize();
        }
        holder.tvMeta.setText(meta);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });

        holder.btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMenu);
            popup.inflate(R.menu.menu_recent_item);
            popup.setOnMenuItemClickListener(menuItem -> {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.menu_open) {
                    if (listener != null) listener.onOpen(item);
                    return true;
                } else if (itemId == R.id.menu_share) {
                    if (listener != null) listener.onShare(item);
                    return true;
                } else if (itemId == R.id.menu_delete) {
                    if (listener != null) listener.onDelete(item);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvMeta;
        ImageButton btnMenu;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_recent_name);
            tvMeta = itemView.findViewById(R.id.tv_recent_meta);
            btnMenu = itemView.findViewById(R.id.btn_recent_menu);
        }
    }
}
