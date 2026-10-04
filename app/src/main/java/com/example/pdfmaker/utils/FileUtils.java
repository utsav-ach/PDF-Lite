package com.example.pdfmaker.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileUtils {

    public static final String FILE_PROVIDER_AUTHORITY = "com.example.pdfmaker.fileprovider";

    /**
     * Gets or creates the app's dedicated Documents directory on external storage.
     */
    public static File getAppPdfDirectory(Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) {
            dir = new File(context.getFilesDir(), "Documents");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Returns human-readable file size (e.g. 840 KB, 2.4 MB).
     */
    public static String getFormattedFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Returns human-friendly relative date (Today, Yesterday, or MMM d, yyyy).
     */
    public static String getFormattedDate(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String timeStr = timeFormat.format(new Date(timestamp));

        if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "Today, " + timeStr;
        } else if (cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday, " + timeStr;
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            return dateFormat.format(new Date(timestamp));
        }
    }

    /**
     * Generates a sensible default filename with timestamp.
     */
    public static String generateDefaultPdfName(String prefix) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return prefix + "_" + timestamp;
    }

    /**
     * Ensures clean filename ending with .pdf and no illegal filesystem characters.
     */
    public static String sanitizeFileName(String inputName, String fallbackPrefix) {
        if (inputName == null || inputName.trim().isEmpty()) {
            inputName = generateDefaultPdfName(fallbackPrefix);
        }
        String clean = inputName.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!clean.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            clean += ".pdf";
        }
        return clean;
    }

    /**
     * Extracts display name from Content URI.
     */
    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        if (result == null) {
            result = "document.pdf";
        }
        return result;
    }

    /**
     * Gets secure content URI via FileProvider for sharing or viewing.
     */
    public static Uri getFileUri(Context context, File file) {
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file);
    }

    /**
     * Copies content URI stream to a temporary cache file.
     */
    public static File copyUriToTempFile(Context context, Uri uri, String prefix, String extension) {
        try {
            File tempFile = File.createTempFile(prefix, extension, context.getCacheDir());
            tempFile.deleteOnExit();
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tempFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely deletes a file.
     */
    public static boolean deleteFile(File file) {
        return file != null && file.exists() && file.delete();
    }

    /**
     * Returns all created PDF files in the app's documents directory sorted by last modified (newest first).
     */
    public static List<File> getRecentPdfFiles(Context context) {
        File dir = getAppPdfDirectory(context);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".pdf"));
        List<File> result = new ArrayList<>();
        if (files != null && files.length > 0) {
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            result.addAll(Arrays.asList(files));
        }
        return result;
    }
}
