package com.example.pdfmaker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.adapter.RecentDocsAdapter;
import com.example.pdfmaker.model.RecentDocItem;
import com.example.pdfmaker.pdf.PdfGenerator;
import com.example.pdfmaker.utils.DialogUtils;
import com.example.pdfmaker.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvRecentDocs;
    private TextView tvRecentEmpty;

    private final List<RecentDocItem> recentDocs = new ArrayList<>();
    private RecentDocsAdapter recentAdapter;
    private ExecutorService backgroundExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backgroundExecutor = Executors.newSingleThreadExecutor();

        initActionCards();
        initRecentDocuments();
    }

    private void initActionCards() {
        View cardScan = findViewById(R.id.card_scan_doc);
        View cardPhotos = findViewById(R.id.card_photos_pdf);
        View cardExtract = findViewById(R.id.card_extract_pages);

        cardScan.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        });

        cardPhotos.setOnClickListener(v -> {
            Intent intent = new Intent(this, PhotoPickerActivity.class);
            startActivity(intent);
        });

        cardExtract.setOnClickListener(v -> {
            Intent intent = new Intent(this, PdfPageSelectionActivity.class);
            startActivity(intent);
        });
    }

    private void initRecentDocuments() {
        rvRecentDocs = findViewById(R.id.rv_recent_docs);
        tvRecentEmpty = findViewById(R.id.tv_recent_empty);

        recentAdapter = new RecentDocsAdapter(this, recentDocs, new RecentDocsAdapter.OnDocActionListener() {
            @Override
            public void onOpen(RecentDocItem item) {
                openPdf(item.getFile());
            }

            @Override
            public void onShare(RecentDocItem item) {
                sharePdf(item.getFile());
            }

            @Override
            public void onDelete(RecentDocItem item) {
                DialogUtils.showDeleteConfirmDialog(MainActivity.this, item.getName(), () -> {
                    FileUtils.deleteFile(item.getFile());
                    loadRecentDocuments();
                });
            }

            @Override
            public void onItemClick(RecentDocItem item) {
                Intent intent = new Intent(MainActivity.this, PreviewActivity.class);
                intent.putExtra(PreviewActivity.EXTRA_PDF_PATH, item.getFile().getAbsolutePath());
                startActivity(intent);
            }
        });

        rvRecentDocs.setLayoutManager(new LinearLayoutManager(this));
        rvRecentDocs.setAdapter(recentAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentDocuments();
    }

    private void loadRecentDocuments() {
        backgroundExecutor.execute(() -> {
            List<File> files = FileUtils.getRecentPdfFiles(this);
            List<RecentDocItem> items = new ArrayList<>();

            for (File file : files) {
                String name = file.getName();
                String formattedDate = FileUtils.getFormattedDate(file.lastModified());
                String formattedSize = FileUtils.getFormattedFileSize(file.length());
                int pageCount = PdfGenerator.getPdfPageCount(file);

                items.add(new RecentDocItem(file, name, formattedDate, formattedSize, pageCount, file.lastModified()));
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                recentDocs.clear();
                recentDocs.addAll(items);
                recentAdapter.notifyDataSetChanged();

                if (recentDocs.isEmpty()) {
                    rvRecentDocs.setVisibility(View.GONE);
                    tvRecentEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvRecentDocs.setVisibility(View.VISIBLE);
                    tvRecentEmpty.setVisibility(View.GONE);
                }
            });
        });
    }

    private void openPdf(File file) {
        try {
            Uri uri = FileUtils.getFileUri(this, file);
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

    private void sharePdf(File file) {
        try {
            Uri uri = FileUtils.getFileUri(this, file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
        }
    }
}
