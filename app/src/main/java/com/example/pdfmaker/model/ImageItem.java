package com.example.pdfmaker.model;

import android.net.Uri;

public class ImageItem {
    private final Uri uri;
    private final String displayName;
    private final long sizeBytes;

    public ImageItem(Uri uri, String displayName, long sizeBytes) {
        this.uri = uri;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
    }

    public Uri getUri() {
        return uri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
