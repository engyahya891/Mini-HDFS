package com.hdfs.common.protocol;

public class BlockAllocation {
    private int blockIndex;   // رقم القطعة (0, 1, 2...)
    private String workerUrl; // عنوان الووركر المسؤول عنها

    public BlockAllocation() {}

    public BlockAllocation(int blockIndex, String workerUrl) {
        this.blockIndex = blockIndex;
        this.workerUrl = workerUrl;
    }

    public int getBlockIndex() { return blockIndex; }
    public String getWorkerUrl() { return workerUrl; }
}