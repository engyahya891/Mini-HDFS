package com.hdfs.namenode.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "files")
public class FileMetadata {

    @Id
    private String filename;

    private long fileSize;

    // 🟢 التعديل الجديد: إضافة حقل المالك (Owner)
    // هذا الحقل سيخزن اسم المستخدم الذي رفع الملف
    @Column(nullable = false)
    private String owner;

    // 🟢 قائمة البلوكات (كما هي)
    @OneToMany(mappedBy = "fileMetadata", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<BlockMetadata> blocks = new ArrayList<>();

    public FileMetadata() {}

    // تم تحديث الـ Constructor لاستقبال اسم المالك أيضاً
    public FileMetadata(String filename, long fileSize, String owner) {
        this.filename = filename;
        this.fileSize = fileSize;
        this.owner = owner;
    }

    // دالة مساعدة لإضافة بلوك جديد
    public void addBlock(BlockMetadata block) {
        this.blocks.add(block);
        block.setFileMetadata(this); // ربط البلوك بالملف (Bi-directional)
    }

    // --- Getters ---
    public String getFilename() { return filename; }
    public long getFileSize() { return fileSize; }
    public String getOwner() { return owner; } // Getter الجديد
    public List<BlockMetadata> getBlocks() { return blocks; }

    // --- Setters ---
    public void setFilename(String filename) { this.filename = filename; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setOwner(String owner) { this.owner = owner; } // Setter الجديد
    public void setBlocks(List<BlockMetadata> blocks) { this.blocks = blocks; }
}