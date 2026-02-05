package com.hdfs.common.protocol;

public class HeartbeatRequest {
    private String workerId;  // هذا سيحمل رابط الوركر (URL)
    private long usedSpace;   // المساحة المستخدمة
    private long totalSpace;  // 🟢 الإضافة الجديدة: المساحة الكلية

    public HeartbeatRequest() {}

    public HeartbeatRequest(String workerId, long usedSpace, long totalSpace) {
        this.workerId = workerId;
        this.usedSpace = usedSpace;
        this.totalSpace = totalSpace;
    }

    // Getters and Setters
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public long getUsedSpace() { return usedSpace; }
    public void setUsedSpace(long usedSpace) { this.usedSpace = usedSpace; }

    public long getTotalSpace() { return totalSpace; }
    public void setTotalSpace(long totalSpace) { this.totalSpace = totalSpace; }
}