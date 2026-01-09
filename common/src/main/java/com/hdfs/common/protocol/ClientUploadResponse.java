package com.hdfs.common.protocol;

// هذا الكلاس يمثل "رد الماستر"
// الماستر سيخبر العميل: "حسناً، اذهب وارسل الملف لهذا الووركر"
public class ClientUploadResponse {
    private boolean success;      // هل وافق الماستر أم لا؟
    private String workerUrl;     // عنوان الووركر الذي سيستقبل الملف (مثلاً: http://localhost:8081)
    private String ticketId;      // (اختياري) رقم تذكرة للسماح بالرفع

    public ClientUploadResponse() {}

    public ClientUploadResponse(boolean success, String workerUrl) {
        this.success = success;
        this.workerUrl = workerUrl;
    }

    // Getters & Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getWorkerUrl() { return workerUrl; }
    public void setWorkerUrl(String workerUrl) { this.workerUrl = workerUrl; }
}