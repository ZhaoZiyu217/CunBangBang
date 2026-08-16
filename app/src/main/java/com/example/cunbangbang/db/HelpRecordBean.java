package com.example.cunbangbang.db;

import java.io.Serializable;

public class HelpRecordBean implements Serializable {
    private int id;
    private String helperName;
    private String helperVillage;
    private long timestamp;
    private String fileName;
    private String fileUrl;
    private String status;

    public HelpRecordBean() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getHelperName() { return helperName; }
    public void setHelperName(String helperName) { this.helperName = helperName; }

    public String getHelperVillage() { return helperVillage; }
    public void setHelperVillage(String helperVillage) { this.helperVillage = helperVillage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
