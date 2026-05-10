package com.hdfs.common.protocol;

public class HeartbeatRequest {
    private String workerId;
    private long usedSpace;
    private long totalSpace;
    private String rackId;
    // 🟢 الحقول الجديدة للأداء
    private double cpuUsage;
    private double ramUsage;

    // المُنشئ (Constructor) القديم والجديد
    public HeartbeatRequest() {}

    public HeartbeatRequest(String workerId, long usedSpace, long totalSpace, double cpuUsage, double ramUsage) {
        this.workerId = workerId;
        this.usedSpace = usedSpace;
        this.totalSpace = totalSpace;
        this.cpuUsage = cpuUsage;
        this.ramUsage = ramUsage;
    }

    // Getters and Setters
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public long getUsedSpace() { return usedSpace; }
    public void setUsedSpace(long usedSpace) { this.usedSpace = usedSpace; }

    public long getTotalSpace() { return totalSpace; }
    public void setTotalSpace(long totalSpace) { this.totalSpace = totalSpace; }

    // 🟢 Getters and Setters الجديدة
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public double getRamUsage() { return ramUsage; }
    public void setRamUsage(double ramUsage) { this.ramUsage = ramUsage; }

    public String getRackId() { return rackId; }
    public void setRackId(String rackId) { this.rackId = rackId; }
}