package com.example.pdfmaker;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.adapter.ImageThumbAdapter;
import com.example.pdfmaker.model.ImageItem;
import com.example.pdfmaker.pdf.PdfGenerator;
import com.example.pdfmaker.utils.DialogUtils;
import com.example.pdfmaker.utils.FileUtils;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoPickerActivity extends AppCompatActivity {

    private RecyclerView rvSelectedPhotos;
    private View layoutPickerEmpty;
    private TextView tvPhotoCountHint;
    private MaterialButton btnCreatePdf;
    private MaterialButton btnAddMore;

    private final List<ImageItem> selectedImages = new ArrayList<>();
    private ImageThumbAdapter adapter;
    private ExecutorService executorService;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    List<ImageItem> newItems = new ArrayList<>();
                    for (Uri uri : uris) {
                        String displayName = FileUtils.getFileNameFromUri(this, uri);
                        newItems.add(new ImageItem(uri, displayName, 0));
                    }
                    adapter.addImages(newItems);
                    updateUI();
                } else if (selectedImages.isEmpty()) {
                    updateUI();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);

        executorService = Executors.newSingleThreadExecutor();

        initViews();
        // Immediately launch photo picker when activity starts
        launchPhotoPicker();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_picker_back);
        btnAddMore = findViewById(R.id.btn_add_more);
        rvSelectedPhotos = findViewById(R.id.rv_selected_photos);
        layoutPickerEmpty = findViewById(R.id.layout_picker_empty);
        tvPhotoCountHint = findViewById(R.id.tv_photo_count_hint);
        btnCreatePdf = findViewById(R.id.btn_create_pdf_from_photos);
        MaterialButton btnEmptyAdd = findViewById(R.id.btn_picker_add_empty);

        btnBack.setOnClickListener(v -> finish());
        btnAddMore.setOnClickListener(v -> launchPhotoPicker());
        btnEmptyAdd.setOnClickListener(v -> launchPhotoPicker());

        adapter = new ImageThumbAdapter(this, selectedImages, totalCount -> updateUI());

        rvSelectedPhotos.setLayoutManager(new LinearLayoutManager(this));
        rvSelectedPhotos.setAdapter(adapter);

        btnCreatePdf.setOnClickListener(v -> onCreatePdfClicked());

        updateUI();
    }

    private void launchPhotoPicker() {
        pickMultipleMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void updateUI() {
        int count = selectedImages.size();
        if (count == 0) {
            rvSelectedPhotos.setVisibility(View.GONE);
            layoutPickerEmpty.setVisibility(View.VISIBLE);
            btnCreatePdf.setEnabled(false);
            btnCreatePdf.setAlpha(0.4f);
            tvPhotoCountHint.setText(R.string.no_photos_selected);
        } else {
            rvSelectedPhotos.setVisibility(View.VISIBLE);
            layoutPickerEmpty.setVisibility(View.GONE);
            btnCreatePdf.setEnabled(true);
            btnCreatePdf.setAlpha(1.0f);
            String photosText = count + (count == 1 ? " photo" : " photos");
            btnCreatePdf.setText(getString(R.string.create_pdf) + " (" + photosText + ")");
            tvPhotoCountHint.setText(photosText + " selected • " + getString(R.string.reorder_hint));
        }
    }

    private void onCreatePdfClicked() {
        if (selectedImages.isEmpty()) return;

        String defaultName = FileUtils.generateDefaultPdfName("Photos");
        DialogUtils.showFilenameDialog(this, defaultName, "Photos", finalFilename -> {
            generatePdf(finalFilename);
        });
    }

    private void generatePdf(String filename) {
        Dialog progressDialog = DialogUtils.showProgressDialog(this, getString(R.string.creating_pdf_progress));

        File outputDir = FileUtils.getAppPdfDirectory(this);
        File outputFile = new File(outputDir, filename);

        List<Uri> uris = new ArrayList<>();
        for (ImageItem item : selectedImages) {
            uris.add(item.getUri());
        }

        executorService.execute(() -> {
            boolean success = PdfGenerator.createPdfFromImages(this, uris, outputFile, null);

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
    }
}
