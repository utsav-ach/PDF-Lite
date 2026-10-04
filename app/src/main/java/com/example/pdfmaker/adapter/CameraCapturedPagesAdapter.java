package com.example.pdfmaker.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.R;

import java.io.File;
import java.util.List;

public class CameraCapturedPagesAdapter extends RecyclerView.Adapter<CameraCapturedPagesAdapter.ViewHolder> {

    public interface OnPageDeletedListener {
        void onPageDeleted(int position);
    }

    private final Context context;
    private final List<File> pageFiles;
    private final OnPageDeletedListener listener;

    public CameraCapturedPagesAdapter(Context context, List<File> pageFiles, OnPageDeletedListener listener) {
        this.context = context;
        this.pageFiles = pageFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_camera_captured_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = pageFiles.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.ivThumb.setImageURI(Uri.fromFile(file));

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onPageDeleted(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pageFiles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvIndex;
        ImageView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_captured_thumb);
            tvIndex = itemView.findViewById(R.id.tv_page_index);
            btnDelete = itemView.findViewById(R.id.btn_delete_captured);
        }
    }
}
