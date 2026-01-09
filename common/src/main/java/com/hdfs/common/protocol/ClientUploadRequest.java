package com.hdfs.common.protocol;

// هذا الكلاس يمثل "طلب الرفع" الذي يرسله العميل للماستر
public class ClientUploadRequest {
    private String filename; // اسم الملف الذي يريد المستخدم رفعه
    private long fileSize;   // حجم الملف بالبايت (لأن الماستر يحتاج معرفة الحجم ليحجز مساحة)

    public ClientUploadRequest() {}

    public ClientUploadRequest(String filename, long fileSize) {
        this.filename = filename;
        this.fileSize = fileSize;
    }

    // دوال الوصول (Getters & Setters)
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}