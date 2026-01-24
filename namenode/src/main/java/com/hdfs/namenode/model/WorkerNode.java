package com.hdfs.namenode.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class WorkerNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url; // مثال: http://localhost:8081

    private boolean active = true; // هل هو يعمل أم معطل؟

    // Constructors
    public WorkerNode() {}

    public WorkerNode(String url) {
        this.url = url;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}