package com.hdfs.client; // انتبه لاسم الباكيج

public class BlockAllocation {
    private int blockIndex;
    private String workerUrl;

    // يجب وجود Empty Constructor لعمل JSON
    public BlockAllocation() {}

    public BlockAllocation(int blockIndex, String workerUrl) {
        this.blockIndex = blockIndex;
        this.workerUrl = workerUrl;
    }

    public int getBlockIndex() { return blockIndex; }
    public String getWorkerUrl() { return workerUrl; }
}