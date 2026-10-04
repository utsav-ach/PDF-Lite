package com.example.pdfmaker.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import android.media.ExifInterface;

import java.io.InputStream;

public class ImageUtils {

    /**
     * Calculates the best inSampleSize to scale the image down to within maxW and maxH.
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Loads a downsampled bitmap safely from a Uri for PDF generation (max 2048x2048 for high print clarity without OOM).
     */
    public static Bitmap loadBitmapForPdf(Context context, Uri uri, int maxDimension) {
        try {
            // First decode bounds
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, options);
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null;
            }

            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Uses half memory of ARGB_8888, excellent for docs

            Bitmap bitmap;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                bitmap = BitmapFactory.decodeStream(in, null, options);
            }

            if (bitmap == null) return null;

            // Handle EXIF orientation
            int rotation = getExifOrientation(context, uri);
            if (rotation != 0) {
                Bitmap rotated = rotateBitmap(bitmap, rotation);
                if (rotated != bitmap) {
                    bitmap.recycle();
                    bitmap = rotated;
                }
            }

            return bitmap;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * Loads a small thumbnail bitmap for UI display (e.g. 160x160).
     */
    public static Bitmap loadThumbnail(Context context, Uri uri, int thumbSize) {
        return loadBitmapForPdf(context, uri, thumbSize);
    }

    /**
     * Reads EXIF rotation from image stream.
     */
    private static int getExifOrientation(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return 0;
            ExifInterface exifInterface = new ExifInterface(in);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Rotates a bitmap by specified degrees.
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || bitmap == null) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * Safely recycles a bitmap if not already recycled.
     */
    public static void safeRecycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
