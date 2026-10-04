package com.example.pdfmaker;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.adapter.CameraCapturedPagesAdapter;
import com.example.pdfmaker.pdf.PdfGenerator;
import com.example.pdfmaker.utils.DialogUtils;
import com.example.pdfmaker.utils.FileUtils;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageButton btnFlash;
    private ImageButton btnShutter;
    private MaterialButton btnDone;
    private TextView tvScanStatus;
    private TextView tvPageCounter;
    private RecyclerView rvCapturedPages;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;
    private boolean isFlashOn = false;

    private final List<File> capturedPageFiles = new ArrayList<>();
    private CameraCapturedPagesAdapter pagesAdapter;
    private File tempDir;
    private ExecutorService cameraExecutor;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        tempDir = new File(getCacheDir(), "scanned_pages");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        initViews();
        checkPermissionsAndStartCamera();
    }

    private void initViews() {
        previewView = findViewById(R.id.preview_view);
        ImageButton btnBack = findViewById(R.id.btn_camera_back);
        btnFlash = findViewById(R.id.btn_camera_flash);
        btnShutter = findViewById(R.id.btn_capture_shutter);
        btnDone = findViewById(R.id.btn_camera_done);
        tvScanStatus = findViewById(R.id.tv_scan_status);
        tvPageCounter = findViewById(R.id.tv_page_counter);
        rvCapturedPages = findViewById(R.id.rv_captured_pages);

        btnBack.setOnClickListener(v -> finish());

        btnFlash.setOnClickListener(v -> toggleFlash());

        btnShutter.setOnClickListener(v -> capturePage());

        btnDone.setOnClickListener(v -> onDoneScanning());

        pagesAdapter = new CameraCapturedPagesAdapter(this, capturedPageFiles, position -> {
            if (position >= 0 && position < capturedPageFiles.size()) {
                File deleted = capturedPageFiles.remove(position);
                if (deleted != null && deleted.exists()) {
                    deleted.delete();
                }
                pagesAdapter.notifyDataSetChanged();
                updateUIState();
            }
        });

        rvCapturedPages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCapturedPages.setAdapter(pagesAdapter);

        updateUIState();
    }

    private void checkPermissionsAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Toast.makeText(this, "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleFlash() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, "Flash unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        isFlashOn = !isFlashOn;
        camera.getCameraControl().enableTorch(isFlashOn);
        btnFlash.setImageResource(isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
        if (imageCapture != null) {
            imageCapture.setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
        }
    }

    private void capturePage() {
        if (imageCapture == null) return;

        btnShutter.setEnabled(false);

        String pageFileName = "page_" + System.currentTimeMillis() + ".jpg";
        File photoFile = new File(tempDir, pageFileName);

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        btnShutter.setEnabled(true);
                        capturedPageFiles.add(photoFile);
                        pagesAdapter.notifyItemInserted(capturedPageFiles.size() - 1);
                        rvCapturedPages.smoothScrollToPosition(capturedPageFiles.size() - 1);
                        updateUIState();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        btnShutter.setEnabled(true);
                        Toast.makeText(CameraActivity.this, "Failed to capture: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void updateUIState() {
        int count = capturedPageFiles.size();
        if (count == 0) {
            rvCapturedPages.setVisibility(View.GONE);
            btnDone.setVisibility(View.GONE);
            tvScanStatus.setText("Align document inside frame and capture");
            tvPageCounter.setText("0 pages");
        } else {
            rvCapturedPages.setVisibility(View.VISIBLE);
            btnDone.setVisibility(View.VISIBLE);
            btnDone.setText(getString(R.string.scan_done) + " (" + count + ")");
            tvScanStatus.setText("Tap shutter for next page or Done to create PDF");
            tvPageCounter.setText(count + (count == 1 ? " page" : " pages"));
        }
    }

    private void onDoneScanning() {
        if (capturedPageFiles.isEmpty()) return;

        String defaultName = FileUtils.generateDefaultPdfName("Scan");
        DialogUtils.showFilenameDialog(this, defaultName, "Scan", finalFilename -> {
            generatePdf(finalFilename);
        });
    }

    private void generatePdf(String filename) {
        Dialog progressDialog = DialogUtils.showProgressDialog(this, getString(R.string.creating_pdf_progress));

        File outputDir = FileUtils.getAppPdfDirectory(this);
        File outputFile = new File(outputDir, filename);

        List<Uri> uris = new ArrayList<>();
        for (File f : capturedPageFiles) {
            uris.add(Uri.fromFile(f));
        }

        cameraExecutor.execute(() -> {
            boolean success = PdfGenerator.createPdfFromImages(this, uris, outputFile, null);

            // Clean up temporary camera page files
            for (File f : capturedPageFiles) {
                if (f.exists()) f.delete();
            }

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
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
