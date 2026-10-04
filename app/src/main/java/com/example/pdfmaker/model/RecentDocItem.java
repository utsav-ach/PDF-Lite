package com.example.pdfmaker.model;

import java.io.File;

public class RecentDocItem {
    private final File file;
    private final String name;
    private final String formattedDate;
    private final String formattedSize;
    private final int pageCount;
    private final long lastModified;

    public RecentDocItem(File file, String name, String formattedDate, String formattedSize, int pageCount, long lastModified) {
        this.file = file;
        this.name = name;
        this.formattedDate = formattedDate;
        this.formattedSize = formattedSize;
        this.pageCount = pageCount;
        this.lastModified = lastModified;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public String getFormattedSize() {
        return formattedSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public long getLastModified() {
        return lastModified;
    }
}
