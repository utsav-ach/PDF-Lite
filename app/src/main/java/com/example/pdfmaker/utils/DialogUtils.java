package com.example.pdfmaker.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.pdfmaker.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DialogUtils {

    public interface OnFilenameConfirmedListener {
        void onConfirmed(String finalFilename);
    }

    /**
     * Prompts the user with a clean filename dialog pre-filled with a sensible default name.
     */
    public static void showFilenameDialog(Context context, String defaultName, String fallbackPrefix, OnFilenameConfirmedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_filename, null);
        EditText etFilename = view.findViewById(R.id.et_filename);

        if (defaultName != null && !defaultName.trim().isEmpty()) {
            if (defaultName.toLowerCase().endsWith(".pdf")) {
                defaultName = defaultName.substring(0, defaultName.length() - 4);
            }
            etFilename.setText(defaultName);
            etFilename.selectAll();
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setPositiveButton(R.string.save, (d, which) -> {
                    String input = etFilename.getText().toString();
                    String cleanName = FileUtils.sanitizeFileName(input, fallbackPrefix);
                    listener.onConfirmed(cleanName);
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.show();
        etFilename.requestFocus();
    }

    /**
     * Shows a clean loading/progress dialog while generating PDF.
     */
    public static Dialog showProgressDialog(Context context, String message) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(48, 48, 48, 48);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        layout.addView(progressBar);

        TextView textView = new TextView(context);
        textView.setText(message);
        textView.setPadding(36, 0, 0, 0);
        textView.setTextSize(15);
        textView.setTextColor(context.getResources().getColor(R.color.text_primary));
        layout.addView(textView);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(layout)
                .setCancelable(false)
                .create();

        dialog.show();
        return dialog;
    }

    /**
     * Confirmation dialog before deleting a PDF document.
     */
    public static void showDeleteConfirmDialog(Context context, String fileName, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(context.getString(R.string.delete_confirm_msg, fileName))
                .setPositiveButton(R.string.delete, (d, which) -> {
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
