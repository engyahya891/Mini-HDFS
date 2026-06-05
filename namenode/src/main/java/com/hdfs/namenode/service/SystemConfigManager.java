package com.hdfs.namenode.service;

import org.springframework.stereotype.Component;

@Component
public class SystemConfigManager {
    private int blockSizeMB = 64;
    private int replicationFactor = 3;
    private String adminPassword = "admin1234";

    public int getBlockSizeMB() { return blockSizeMB; }
    public void setBlockSizeMB(int blockSizeMB) { this.blockSizeMB = blockSizeMB; }

    public int getReplicationFactor() { return replicationFactor; }
    public void setReplicationFactor(int replicationFactor) { this.replicationFactor = replicationFactor; }

    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
}