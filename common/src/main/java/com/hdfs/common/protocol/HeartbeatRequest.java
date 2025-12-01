package com.hdfs.common.protocol;

public class HeartbeatRequest {
    private String workerId;
    private long usedSpace; // المساحة المستخدمة بالبايت

    public HeartbeatRequest() {}

    public HeartbeatRequest(String workerId, long usedSpace) {
        this.workerId = workerId;
        this.usedSpace = usedSpace;
    }

    // Getters and Setters
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public long getUsedSpace() { return usedSpace; }
    public void setUsedSpace(long usedSpace) { this.usedSpace = usedSpace; }
}