package com.hdfs.namenode.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "blocks")
public class BlockMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String blockId; // مثال: video.mp4_part_1

    private int blockIndex;

    private String workerUrl;

    // ✅ 1. هذا هو الحقل الذي نستخدمه الآن في التقرير (String)
    // سيتم إنشاء عمود اسمه "filename" في الجدول
    private String filename;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    @JsonIgnore
    private WorkerNode worker;

    // ✅ 2. الإصلاح هنا: غيرنا الاسم إلى "file_id" لمنع التضارب مع الحقل النصي أعلاه
    @ManyToOne
    @JoinColumn(name = "file_id")
    @JsonIgnore
    private FileMetadata fileMetadata;

    // --- Constructors ---
    public BlockMetadata() {}

    // --- Getters ---
    public Long getId() { return id; }
    public String getBlockId() { return blockId; }
    public int getBlockIndex() { return blockIndex; }
    public String getWorkerUrl() { return workerUrl; }
    public String getFilename() { return filename; }
    public WorkerNode getWorker() { return worker; }
    public FileMetadata getFileMetadata() { return fileMetadata; }

    // --- Setters ---
    public void setId(Long id) { this.id = id; }

    public void setBlockId(String blockId) { this.blockId = blockId; }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public void setWorkerUrl(String workerUrl) {
        this.workerUrl = workerUrl;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setWorker(WorkerNode worker) {
        this.worker = worker;
    }

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }
}