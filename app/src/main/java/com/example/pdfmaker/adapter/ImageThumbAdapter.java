package com.example.pdfmaker.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.R;
import com.example.pdfmaker.model.ImageItem;
import com.example.pdfmaker.utils.ImageUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageThumbAdapter extends RecyclerView.Adapter<ImageThumbAdapter.ViewHolder> {

    public interface OnListChangedListener {
        void onListChanged(int totalCount);
    }

    private final Context context;
    private final List<ImageItem> items;
    private final OnListChangedListener listener;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Bitmap> thumbCache = new HashMap<>();

    public ImageThumbAdapter(Context context, List<ImageItem> items, OnListChangedListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_thumb, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageItem item = items.get(position);
        int pageNumber = position + 1;

        holder.tvPageTitle.setText("Page " + pageNumber);
        holder.tvImageInfo.setText(item.getDisplayName());

        // Thumbnail loading
        String uriKey = item.getUri().toString();
        if (thumbCache.containsKey(uriKey)) {
            holder.ivThumb.setImageBitmap(thumbCache.get(uriKey));
        } else {
            holder.ivThumb.setImageDrawable(null);
            executor.execute(() -> {
                Bitmap thumb = ImageUtils.loadThumbnail(context, item.getUri(), 160);
                if (thumb != null) {
                    thumbCache.put(uriKey, thumb);
                    mainHandler.post(() -> {
                        int currentPos = holder.getAdapterPosition();
                        if (currentPos != RecyclerView.NO_POSITION && currentPos < items.size() &&
                                items.get(currentPos).getUri().toString().equals(uriKey)) {
                            holder.ivThumb.setImageBitmap(thumb);
                        }
                    });
                }
            });
        }

        // Reordering controls
        holder.btnMoveUp.setEnabled(position > 0);
        holder.btnMoveUp.setAlpha(position > 0 ? 1.0f : 0.25f);
        holder.btnMoveUp.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos > 0) {
                Collections.swap(items, pos, pos - 1);
                notifyItemMoved(pos, pos - 1);
                notifyItemChanged(pos);
                notifyItemChanged(pos - 1);
            }
        });

        holder.btnMoveDown.setEnabled(position < items.size() - 1);
        holder.btnMoveDown.setAlpha(position < items.size() - 1 ? 1.0f : 0.25f);
        holder.btnMoveDown.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos < items.size() - 1) {
                Collections.swap(items, pos, pos + 1);
                notifyItemMoved(pos, pos + 1);
                notifyItemChanged(pos);
                notifyItemChanged(pos + 1);
            }
        });

        // Removal
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < items.size()) {
                items.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, items.size() - pos);
                if (listener != null) {
                    listener.onListChanged(items.size());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addImages(List<ImageItem> newItems) {
        int startPos = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(startPos, newItems.size());
        if (listener != null) {
            listener.onListChanged(items.size());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvPageTitle;
        TextView tvImageInfo;
        ImageButton btnMoveUp;
        ImageButton btnMoveDown;
        ImageButton btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_thumb);
            tvPageTitle = itemView.findViewById(R.id.tv_page_title);
            tvImageInfo = itemView.findViewById(R.id.tv_image_info);
            btnMoveUp = itemView.findViewById(R.id.btn_move_up);
            btnMoveDown = itemView.findViewById(R.id.btn_move_down);
            btnRemove = itemView.findViewById(R.id.btn_remove_photo);
        }
    }
}
