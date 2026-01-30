package com.hdfs.common.protocol; // تأكد أن الباكيج صحيح

import java.util.List; // 🟢 إضافة مهمة

public class BlockAllocation {
    private int blockIndex;

    // 🔴 التغيير هنا: بدلاً من workerUrl واحد، نجعلها قائمة
    // private String workerUrl;  <-- احذف هذا
    private List<String> workerUrls; // <-- ضع هذا

    // Empty Constructor (مهم للـ JSON)
    public BlockAllocation() {}

    // Constructor المعدل
    public BlockAllocation(int blockIndex, List<String> workerUrls) {
        this.blockIndex = blockIndex;
        this.workerUrls = workerUrls;
    }

    public int getBlockIndex() { return blockIndex; }

    // Getter للقائمة
    public List<String> getWorkerUrls() { return workerUrls; } // عدل الاسم للجمع

    // Setters (اختياري لكن مفضل)
    public void setBlockIndex(int blockIndex) { this.blockIndex = blockIndex; }
    public void setWorkerUrls(List<String> workerUrls) { this.workerUrls = workerUrls; }
}