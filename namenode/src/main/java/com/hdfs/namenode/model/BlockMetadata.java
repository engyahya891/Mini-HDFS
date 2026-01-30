package com.hdfs.namenode.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore; // مهم جداً لمنع الدوران اللانهائي

@Entity
@Table(name = "blocks")
public class BlockMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // توليد ID تلقائي (1, 2, 3...)
    private Long id;

    private int blockIndex; // ترتيب القطعة (0، 1، 2...)
    private String workerUrl; // أين توجد هذه القطعة؟

    // ربط البلوك بالملف الأصلي (Many Blocks -> One File)
    @ManyToOne
    @JoinColumn(name = "filename")
    @JsonIgnore // لكي لا يطبع الملف داخل البلوك عند التحويل لـ JSON
    private FileMetadata fileMetadata;

    public BlockMetadata() {}

    public BlockMetadata(int blockIndex, String workerUrl, FileMetadata fileMetadata) {
        this.blockIndex = blockIndex;
        this.workerUrl = workerUrl;
        this.fileMetadata = fileMetadata;
    }

    // Getters and Setters
    public int getBlockIndex() { return blockIndex; }
    public String getWorkerUrl() { return workerUrl; }
    public FileMetadata getFileMetadata() { return fileMetadata; }
}