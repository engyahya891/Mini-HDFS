package com.hdfs.common.protocol;

// هذا الكلاس موجود في الـ Common
// سيستخدمه الطالب A (لاستقبال الطلب) والطالب B (لإرسال الطلب)
public class WorkerRegisterRequest {
    private int port;          // المنفذ الذي يعمل عليه الووركر
    private String storagePath; // مسار التخزين
    private String workerId;    // معرف فريد (اختياري في البداية)
    private String rackId;
    // يجب إضافة Empty Constructor (مهم جداً لـ JSON)
    public WorkerRegisterRequest() {}

    public WorkerRegisterRequest(int port, String storagePath) {
        this.port = port;
        this.storagePath = storagePath;
    }

    // Getters and Setters (ضرورية جداً لعمل Spring Boot)
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getRackId() { return rackId; }
    public void setRackId(String rackId) { this.rackId = rackId; }
}