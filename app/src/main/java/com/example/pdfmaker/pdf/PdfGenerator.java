package com.example.pdfmaker.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.example.pdfmaker.utils.ImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class PdfGenerator {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    public static final int A4_WIDTH = 595;
    public static final int A4_HEIGHT = 842;
    private static final int PAGE_MARGIN = 20;

    public interface ProgressListener {
        void onProgress(int current, int total);
    }

    /**
     * Generates a PDF file from a list of image Uris.
     * Safely handles large images, correct orientation, and memory release.
     */
    public static boolean createPdfFromImages(Context context, List<Uri> imageUris, File outputFile, ProgressListener listener) {
        if (imageUris == null || imageUris.isEmpty() || outputFile == null) {
            return false;
        }

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setDither(true);

        try {
            int total = imageUris.size();
            for (int i = 0; i < total; i++) {
                if (listener != null) {
                    listener.onProgress(i + 1, total);
                }

                Uri uri = imageUris.get(i);
                // Downsample to max 2048px to preserve high readability while avoiding OOM
                Bitmap bitmap = ImageUtils.loadBitmapForPdf(context, uri, 2048);
                if (bitmap == null) {
                    continue;
                }

                int bw = bitmap.getWidth();
                int bh = bitmap.getHeight();

                // Determine page orientation based on image
                boolean isLandscape = bw > bh;
                int pageWidth = isLandscape ? A4_HEIGHT : A4_WIDTH;
                int pageHeight = isLandscape ? A4_WIDTH : A4_HEIGHT;

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Fill page background with crisp white
                canvas.drawColor(Color.WHITE);

                // Calculate fitted bounds preserving aspect ratio
                float availableWidth = pageWidth - (PAGE_MARGIN * 2);
                float availableHeight = pageHeight - (PAGE_MARGIN * 2);

                float scale = Math.min(availableWidth / bw, availableHeight / bh);
                float destWidth = bw * scale;
                float destHeight = bh * scale;

                float left = (pageWidth - destWidth) / 2f;
                float top = (pageHeight - destHeight) / 2f;

                Rect srcRect = new Rect(0, 0, bw, bh);
                RectF destRect = new RectF(left, top, left + destWidth, top + destHeight);

                canvas.drawBitmap(bitmap, srcRect, destRect, paint);
                document.finishPage(page);

                // Immediately release native memory
                ImageUtils.safeRecycle(bitmap);
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                document.writeTo(fos);
                fos.flush();
            }

            return outputFile.exists() && outputFile.length() > 0;
        } catch (Exception | OutOfMemoryError e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            return false;
        } finally {
            document.close();
        }
    }

    /**
     * Extracts selected pages from an existing PDF and writes them to a new PDF.
     * Uses high-resolution rendering to preserve print and text quality.
     */
    public static boolean extractPagesToPdf(Context context, Uri sourcePdfUri, List<Integer> selectedPages, File outputFile, ProgressListener listener) {
        if (selectedPages == null || selectedPages.isEmpty() || outputFile == null) {
            return false;
        }

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setDither(true);

        ParcelFileDescriptor pfd = null;
        PdfRenderer renderer = null;

        try {
            pfd = context.getContentResolver().openFileDescriptor(sourcePdfUri, "r");
            if (pfd == null) return false;

            renderer = new PdfRenderer(pfd);
            int total = selectedPages.size();

            for (int i = 0; i < total; i++) {
                if (listener != null) {
                    listener.onProgress(i + 1, total);
                }

                int pageIndex = selectedPages.get(i);
                if (pageIndex < 0 || pageIndex >= renderer.getPageCount()) {
                    continue;
                }

                PdfRenderer.Page originalPage = renderer.openPage(pageIndex);
                int pw = originalPage.getWidth();
                int ph = originalPage.getHeight();

                // Render at high resolution (e.g., 2.0x scale or min 1400px on longest side for print clarity)
                float scaleFactor = Math.max(1.5f, Math.min(2.5f, 1800f / Math.max(pw, ph)));
                int renderW = Math.round(pw * scaleFactor);
                int renderH = Math.round(ph * scaleFactor);

                Bitmap pageBitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888);
                // Draw white background
                Canvas renderCanvas = new Canvas(pageBitmap);
                renderCanvas.drawColor(Color.WHITE);

                // Render page to bitmap in PRINT mode
                originalPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                originalPage.close();

                // Create page in new document with original dimensions
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pw, ph, i + 1).create();
                PdfDocument.Page newPage = document.startPage(pageInfo);
                Canvas newCanvas = newPage.getCanvas();

                Rect srcRect = new Rect(0, 0, renderW, renderH);
                RectF destRect = new RectF(0, 0, pw, ph);
                newCanvas.drawBitmap(pageBitmap, srcRect, destRect, paint);

                document.finishPage(newPage);
                ImageUtils.safeRecycle(pageBitmap);
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                document.writeTo(fos);
                fos.flush();
            }

            return outputFile.exists() && outputFile.length() > 0;
        } catch (Exception | OutOfMemoryError e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            return false;
        } finally {
            if (renderer != null) {
                try { renderer.close(); } catch (Exception ignored) {}
            }
            if (pfd != null) {
                try { pfd.close(); } catch (Exception ignored) {}
            }
            document.close();
        }
    }

    /**
     * Reads the total number of pages in a PDF file or Uri.
     */
    public static int getPdfPageCount(Context context, Uri pdfUri) {
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(pdfUri, "r")) {
            if (pfd == null) return 0;
            try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                return renderer.getPageCount();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Reads the total number of pages from a File directly.
     */
    public static int getPdfPageCount(File pdfFile) {
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)) {
            if (pfd == null) return 0;
            try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                return renderer.getPageCount();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Renders a preview thumbnail of the first page of a PDF file.
     */
    public static Bitmap renderFirstPageThumbnail(Context context, File pdfFile, int targetWidth) {
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)) {
            if (pfd == null) return null;
            try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                if (renderer.getPageCount() == 0) return null;
                try (PdfRenderer.Page page = renderer.openPage(0)) {
                    int pw = page.getWidth();
                    int ph = page.getHeight();
                    float ratio = (float) ph / pw;
                    int targetHeight = Math.round(targetWidth * ratio);

                    Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(Color.WHITE);

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    return bitmap;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }
}
