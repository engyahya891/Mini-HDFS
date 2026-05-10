package com.hdfs.namenode.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class WorkerNode {

    @Column(name = "rack_id")
    private String rackId = "Rack-1";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 🟢 يفضل تحديد الاستراتيجية لتجنب مشاكل الترقيم
    private Long id;

    private String url;
    private boolean active;

    private String storagePath;
    private long capacity;
    private long used;
    private double cpuUsage;
    private double ramUsage;



    // 🟢 الإضافة الضرورية: هذا الحقل سيخزن النص "500MB / 1000MB" للعرض السهل
    private String storageInfo;

    private LocalDateTime lastHeartbeat;

    public WorkerNode() {
    }

    public WorkerNode(String url) {
        this.url = url;
        this.active = true;
        this.lastHeartbeat = LocalDateTime.now();
    }

    // --- Getters & Setters ---



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }

    // 🟢 دوال الحقل الجديد
    public String getStorageInfo() {
        return storageInfo;
    }

    public void setStorageInfo(String storageInfo) {
        this.storageInfo = storageInfo;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public double getRamUsage() { return ramUsage; }
    public void setRamUsage(double ramUsage) { this.ramUsage = ramUsage; }

    public String getRackId() { return rackId; }
    public void setRackId(String rackId) { this.rackId = rackId; }

}