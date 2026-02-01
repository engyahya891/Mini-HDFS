package com.hdfs.common.protocol;


import java.util.List;

public class StorageReportRequest {

    private String workerUrl;
    private List<String> blockIds;
    private long capacity;
    private long used;

    // getters & setters

    public String getWorkerUrl() {
        return workerUrl;
    }

    public void setWorkerUrl(String workerUrl) {
        this.workerUrl = workerUrl;
    }

    public List<String> getBlockIds() {
        return blockIds;
    }

    public void setBlockIds(List<String> blockIds) {
        this.blockIds = blockIds;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }
}

