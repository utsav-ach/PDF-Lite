package com.example.pdfmaker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pdfmaker.pdf.PdfGenerator;
import com.example.pdfmaker.utils.FileUtils;

import java.io.File;
import java.util.concurrent.Executors;

public class PreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PDF_PATH = "extra_pdf_path";

    private File pdfFile;
    private TextView tvFilename;
    private TextView tvMeta;
    private ImageView ivPreview;
    private View layoutPreviewThumb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        String path = getIntent().getStringExtra(EXTRA_PDF_PATH);
        if (path == null) {
            finish();
            return;
        }

        pdfFile = new File(path);
        if (!pdfFile.exists()) {
            finish();
            return;
        }

        initViews();
        loadDocumentDetails();
    }

    private void initViews() {
        tvFilename = findViewById(R.id.tv_result_filename);
        tvMeta = findViewById(R.id.tv_result_meta);
        ivPreview = findViewById(R.id.iv_result_preview);
        layoutPreviewThumb = findViewById(R.id.layout_preview_thumb);

        ImageButton btnClose = findViewById(R.id.btn_preview_close);
        Button btnOpen = findViewById(R.id.btn_open_pdf);
        Button btnShare = findViewById(R.id.btn_share_pdf);
        Button btnDoneHome = findViewById(R.id.btn_done_home);

        btnClose.setOnClickListener(v -> navigateToHome());
        btnDoneHome.setOnClickListener(v -> navigateToHome());

        btnOpen.setOnClickListener(v -> openPdf());
        btnShare.setOnClickListener(v -> sharePdf());
    }

    private void loadDocumentDetails() {
        tvFilename.setText(pdfFile.getName());

        long fileSize = pdfFile.length();
        String sizeStr = FileUtils.getFormattedFileSize(fileSize);

        Executors.newSingleThreadExecutor().execute(() -> {
            int pageCount = PdfGenerator.getPdfPageCount(pdfFile);
            Bitmap thumb = PdfGenerator.renderFirstPageThumbnail(this, pdfFile, 320);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (isFinishing() || isDestroyed()) return;

                String pagesStr = pageCount + (pageCount == 1 ? " page" : " pages");
                tvMeta.setText(pagesStr + " • " + sizeStr);

                if (thumb != null) {
                    layoutPreviewThumb.setVisibility(View.VISIBLE);
                    ivPreview.setImageBitmap(thumb);
                } else {
                    layoutPreviewThumb.setVisibility(View.GONE);
                }
            });
        });
    }

    private void openPdf() {
        try {
            Uri uri = FileUtils.getFileUri(this, pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_opening_pdf, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf() {
        try {
            Uri uri = FileUtils.getFileUri(this, pdfFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, pdfFile.getName());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        navigateToHome();
    }
}
