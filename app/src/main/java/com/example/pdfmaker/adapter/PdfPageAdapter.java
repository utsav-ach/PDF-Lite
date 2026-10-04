package com.example.pdfmaker.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.R;
import com.example.pdfmaker.model.PdfPageItem;

import java.util.ArrayList;
import java.util.List;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount, int totalCount);
    }

    private final Context context;
    private final List<PdfPageItem> items;
    private final OnSelectionChangedListener listener;

    public PdfPageAdapter(Context context, List<PdfPageItem> items, OnSelectionChangedListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PdfPageItem item = items.get(position);

        holder.tvPageNumber.setText(context.getString(R.string.scan_add_page).replace("Add Page", "Page " + item.getPageNumber()));
        holder.cbSelect.setChecked(item.isSelected());

        // Visual selection state
        if (item.isSelected()) {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_action_card);
            holder.layoutCard.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.accent_blue_light));
        } else {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_action_card);
            holder.layoutCard.setBackgroundTintList(null);
        }

        // Preview thumbnail
        if (item.getPreviewBitmap() != null) {
            holder.ivPreview.setImageBitmap(item.getPreviewBitmap());
            holder.pbLoading.setVisibility(View.GONE);
        } else {
            holder.ivPreview.setImageDrawable(null);
            holder.pbLoading.setVisibility(View.VISIBLE);
        }

        // Toggle selection on click
        holder.layoutCard.setOnClickListener(v -> {
            item.setSelected(!item.isSelected());
            notifyItemChanged(holder.getAdapterPosition());
            if (listener != null) {
                listener.onSelectionChanged(getSelectedCount(), items.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public int getSelectedCount() {
        int count = 0;
        for (PdfPageItem item : items) {
            if (item.isSelected()) count++;
        }
        return count;
    }

    public List<Integer> getSelectedPageIndices() {
        List<Integer> list = new ArrayList<>();
        for (PdfPageItem item : items) {
            if (item.isSelected()) {
                list.add(item.getPageIndex());
            }
        }
        return list;
    }

    public void selectAll(boolean select) {
        for (PdfPageItem item : items) {
            item.setSelected(select);
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(getSelectedCount(), items.size());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutCard;
        ImageView ivPreview;
        ProgressBar pbLoading;
        CheckBox cbSelect;
        TextView tvPageNumber;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCard = itemView.findViewById(R.id.layout_page_card);
            ivPreview = itemView.findViewById(R.id.iv_page_preview);
            pbLoading = itemView.findViewById(R.id.pb_page_loading);
            cbSelect = itemView.findViewById(R.id.cb_page_select);
            tvPageNumber = itemView.findViewById(R.id.tv_page_number);
        }
    }
}
