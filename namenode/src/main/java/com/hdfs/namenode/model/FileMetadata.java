package com.hdfs.namenode.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "files")
public class FileMetadata {

    @Id
    private String filename; // اسم الملف (Primary Key)

    private String workerUrl;
    private long fileSize;

    public FileMetadata() {} // كونستركتور فارغ إجباري

    public FileMetadata(String filename, String workerUrl, long fileSize) {
        this.filename = filename;
        this.workerUrl = workerUrl;
        this.fileSize = fileSize;
    }

    // Getters
    public String getFilename() { return filename; }
    public String getWorkerUrl() { return workerUrl; }
    public long getFileSize() { return fileSize; }
}