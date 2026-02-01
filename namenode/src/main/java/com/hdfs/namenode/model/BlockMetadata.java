package com.hdfs.namenode.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore; // مهم جداً لمنع الدوران اللانهائي
@Entity
@Table(name = "blocks")
public class BlockMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String blockId;

    private int blockIndex;

    private String workerUrl;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    @JsonIgnore
    private WorkerNode worker;

    @ManyToOne
    @JoinColumn(name = "filename")
    @JsonIgnore
    private FileMetadata fileMetadata;

    public BlockMetadata() {}

    // getters
    public String getBlockId() { return blockId; }
    public int getBlockIndex() { return blockIndex; }
    public String getWorkerUrl() { return workerUrl; }
    public FileMetadata getFileMetadata() { return fileMetadata; }
    public WorkerNode getWorker() { return worker; }

    // setters المهمين للـ report
    public void setBlockId(String blockId) { this.blockId = blockId; }
    public void setWorker(WorkerNode worker) { this.worker = worker; }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public void setWorkerUrl(String workerUrl) {
        this.workerUrl = workerUrl;
    }

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

}
