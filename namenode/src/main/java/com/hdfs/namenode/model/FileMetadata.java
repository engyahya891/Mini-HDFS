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

    // حقل المالك (Owner)
    @Column(nullable = false)
    private String owner;

    // 🟢 الإضافة الجديدة: حقل بصمة التشفير (MD5 Checksum) لضمان سلامة البيانات
    @Column(name = "md5_checksum")
    private String md5Checksum;

    // قفل التزامن المتفائل (Optimistic Locking)
    @Version
    private Long version;

    // قائمة البلوكات
    @OneToMany(mappedBy = "fileMetadata", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<BlockMetadata> blocks = new ArrayList<>();

    public FileMetadata() {}

    // Constructor
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
    public String getOwner() { return owner; }
    public Long getVersion() { return version; }
    public String getMd5Checksum() { return md5Checksum; } // 🟢 Getter الجديد للبصمة
    public List<BlockMetadata> getBlocks() { return blocks; }

    // --- Setters ---
    public void setFilename(String filename) { this.filename = filename; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setVersion(Long version) { this.version = version; }
    public void setMd5Checksum(String md5Checksum) { this.md5Checksum = md5Checksum; } // 🟢 Setter الجديد للبصمة
    public void setBlocks(List<BlockMetadata> blocks) { this.blocks = blocks; }
}