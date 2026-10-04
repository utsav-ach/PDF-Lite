package com.example.pdfmaker;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.adapter.PdfPageAdapter;
import com.example.pdfmaker.model.PdfPageItem;
import com.example.pdfmaker.pdf.PdfGenerator;
import com.example.pdfmaker.utils.DialogUtils;
import com.example.pdfmaker.utils.FileUtils;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfPageSelectionActivity extends AppCompatActivity {

    private RecyclerView rvPages;
    private View layoutLoading;
    private View layoutEmpty;
    private TextView tvDocName;
    private TextView tvSelectionCount;
    private MaterialButton btnSelectAll;
    private MaterialButton btnCreatePdf;

    private Uri sourcePdfUri;
    private String sourcePdfName;
    private final List<PdfPageItem> pageItems = new ArrayList<>();
    private PdfPageAdapter adapter;
    private ExecutorService executorService;
    private boolean allSelected = false;

    private final ActivityResultLauncher<String[]> openPdfLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        // Persist read permission across reboots if possible
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}

                    sourcePdfUri = uri;
                    sourcePdfName = FileUtils.getFileNameFromUri(this, uri);
                    loadPdfPages();
                } else if (pageItems.isEmpty()) {
                    showEmptyState();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_page_selection);

        executorService = Executors.newFixedThreadPool(2);

        initViews();
        // Immediately trigger PDF picker
        launchPdfPicker();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_extract_back);
        btnSelectAll = findViewById(R.id.btn_select_all);
        rvPages = findViewById(R.id.rv_pdf_pages);
        layoutLoading = findViewById(R.id.layout_extract_loading);
        layoutEmpty = findViewById(R.id.layout_extract_empty);
        tvDocName = findViewById(R.id.tv_extract_doc_name);
        tvSelectionCount = findViewById(R.id.tv_extract_count);
        btnCreatePdf = findViewById(R.id.btn_create_extracted_pdf);
        MaterialButton btnPickAnother = findViewById(R.id.btn_pick_another_pdf);

        btnBack.setOnClickListener(v -> finish());
        btnPickAnother.setOnClickListener(v -> launchPdfPicker());

        adapter = new PdfPageAdapter(this, pageItems, (selectedCount, totalCount) -> {
            updateSelectionSummary(selectedCount, totalCount);
        });

        rvPages.setLayoutManager(new GridLayoutManager(this, 2));
        rvPages.setAdapter(adapter);

        btnSelectAll.setOnClickListener(v -> {
            allSelected = !allSelected;
            adapter.selectAll(allSelected);
            btnSelectAll.setText(allSelected ? R.string.clear_selection : R.string.select_all);
        });

        btnCreatePdf.setOnClickListener(v -> onCreatePdfClicked());

        updateSelectionSummary(0, 0);
    }

    private void launchPdfPicker() {
        openPdfLauncher.launch(new String[]{"application/pdf"});
    }

    private void showEmptyState() {
        layoutLoading.setVisibility(View.GONE);
        rvPages.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        btnCreatePdf.setEnabled(false);
        btnCreatePdf.setAlpha(0.4f);
        tvDocName.setText(R.string.no_pdf_loaded);
        tvSelectionCount.setText("");
    }

    private void loadPdfPages() {
        layoutEmpty.setVisibility(View.GONE);
        rvPages.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.VISIBLE);
        tvDocName.setText(sourcePdfName);

        pageItems.clear();
        adapter.notifyDataSetChanged();

        executorService.execute(() -> {
            try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(sourcePdfUri, "r")) {
                if (pfd == null) {
                    showErrorOnMain("Cannot open selected PDF.");
                    return;
                }

                try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                    int totalPages = renderer.getPageCount();
                    if (totalPages == 0) {
                        showErrorOnMain("Selected PDF contains no pages.");
                        return;
                    }

                    List<PdfPageItem> newItems = new ArrayList<>();
                    for (int i = 0; i < totalPages; i++) {
                        newItems.add(new PdfPageItem(i));
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        pageItems.addAll(newItems);
                        adapter.notifyDataSetChanged();
                        layoutLoading.setVisibility(View.GONE);
                        rvPages.setVisibility(View.VISIBLE);
                        updateSelectionSummary(0, totalPages);
                    });

                    // Render thumbnails asynchronously in background
                    for (int i = 0; i < totalPages; i++) {
                        if (isFinishing() || isDestroyed()) break;

                        try (PdfRenderer.Page page = renderer.openPage(i)) {
                            int pw = page.getWidth();
                            int ph = page.getHeight();
                            int targetW = 200;
                            int targetH = Math.round(targetW * ((float) ph / pw));

                            Bitmap bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565);
                            Canvas canvas = new Canvas(bitmap);
                            canvas.drawColor(Color.WHITE);
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                            final int pageIdx = i;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (pageIdx < pageItems.size()) {
                                    pageItems.get(pageIdx).setPreviewBitmap(bitmap);
                                    adapter.notifyItemChanged(pageIdx);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                showErrorOnMain("Failed to read PDF: " + e.getMessage());
            }
        });
    }

    private void showErrorOnMain(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            showEmptyState();
        });
    }

    private void updateSelectionSummary(int selectedCount, int totalCount) {
        if (totalCount == 0) {
            tvSelectionCount.setText("");
            btnCreatePdf.setEnabled(false);
            btnCreatePdf.setAlpha(0.4f);
            return;
        }

        tvSelectionCount.setText(getString(R.string.pages_selected_count, selectedCount, totalCount));

        if (selectedCount > 0) {
            btnCreatePdf.setEnabled(true);
            btnCreatePdf.setAlpha(1.0f);
            String pagesText = selectedCount + (selectedCount == 1 ? " page" : " pages");
            btnCreatePdf.setText(getString(R.string.create_pdf) + " (" + pagesText + ")");
        } else {
            btnCreatePdf.setEnabled(false);
            btnCreatePdf.setAlpha(0.4f);
            btnCreatePdf.setText(R.string.create_pdf);
        }

        allSelected = (selectedCount == totalCount);
        btnSelectAll.setText(allSelected ? R.string.clear_selection : R.string.select_all);
    }

    private void onCreatePdfClicked() {
        List<Integer> selectedIndices = adapter.getSelectedPageIndices();
        if (selectedIndices.isEmpty() || sourcePdfUri == null) return;

        // Sensible default filename: e.g. DocumentName_extracted.pdf
        String baseName = sourcePdfName != null ? sourcePdfName : "Extracted";
        if (baseName.toLowerCase().endsWith(".pdf")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        String defaultName = baseName + "_extracted";

        DialogUtils.showFilenameDialog(this, defaultName, "Extracted", finalFilename -> {
            generatePdf(finalFilename, selectedIndices);
        });
    }

    private void generatePdf(String filename, List<Integer> selectedPages) {
        Dialog progressDialog = DialogUtils.showProgressDialog(this, getString(R.string.creating_pdf_progress));

        File outputDir = FileUtils.getAppPdfDirectory(this);
        File outputFile = new File(outputDir, filename);

        executorService.execute(() -> {
            boolean success = PdfGenerator.extractPagesToPdf(this, sourcePdfUri, selectedPages, outputFile, null);

            new Handler(Looper.getMainLooper()).post(() -> {
                progressDialog.dismiss();
                if (success) {
                    Intent intent = new Intent(this, PreviewActivity.class);
                    intent.putExtra(PreviewActivity.EXTRA_PDF_PATH, outputFile.getAbsolutePath());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, R.string.error_creating_pdf, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        for (PdfPageItem item : pageItems) {
            if (item.getPreviewBitmap() != null && !item.getPreviewBitmap().isRecycled()) {
                item.getPreviewBitmap().recycle();
            }
        }
    }
}
