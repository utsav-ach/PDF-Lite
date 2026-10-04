package com.example.pdfmaker.model;

import android.graphics.Bitmap;

public class PdfPageItem {
    private final int pageIndex; // 0-based index
    private final int pageNumber; // 1-based display number
    private boolean isSelected;
    private Bitmap previewBitmap;
    private boolean isLoading;

    public PdfPageItem(int pageIndex) {
        this.pageIndex = pageIndex;
        this.pageNumber = pageIndex + 1;
        this.isSelected = false;
        this.previewBitmap = null;
        this.isLoading = false;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public Bitmap getPreviewBitmap() {
        return previewBitmap;
    }

    public void setPreviewBitmap(Bitmap previewBitmap) {
        this.previewBitmap = previewBitmap;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }
}
