package com.videocontrol.models;

public class Video {
    private int id;
    private String title;
    private String fileName;
    private String filePath;
    private String extension;
    private long fileSizeBytes;
    private String category;
    private String fileSizeFormatted;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFileSizeFormatted() { return fileSizeFormatted; }
    public void setFileSizeFormatted(String fileSizeFormatted) { this.fileSizeFormatted = fileSizeFormatted; }
}
