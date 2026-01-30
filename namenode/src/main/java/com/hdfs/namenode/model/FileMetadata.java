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

    // 🔴 حذفنا workerUrl القديم
    // private String workerUrl;  <-- تم الحذف

    // 🟢 أضفنا قائمة البلوكات (One File -> Many Blocks)
    @OneToMany(mappedBy = "fileMetadata", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<BlockMetadata> blocks = new ArrayList<>();

    public FileMetadata() {}

    public FileMetadata(String filename, long fileSize) {
        this.filename = filename;
        this.fileSize = fileSize;
    }

    // دالة مساعدة لإضافة بلوك جديد بسهولة
    public void addBlock(BlockMetadata block) {
        this.blocks.add(block);
    }

    // Getters
    public String getFilename() { return filename; }
    public long getFileSize() { return fileSize; }
    public List<BlockMetadata> getBlocks() { return blocks; }
}