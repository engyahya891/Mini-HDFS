package com.hdfs.namenode.model;


/*
* هذا الكلاس يمثل شكل "الرسالة" (Log) كما تتوقعها واجهة React تماماً.
* */
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SystemLog {
    private String id;
    private String timestamp;
    private String level; // INFO, WARN, ERROR
    private String source; // NameNode, DataNode-X, System...
    private String message;

    public SystemLog(String level, String source, String message) {
        this.id = UUID.randomUUID().toString();
        // تنسيق الوقت ليطابق ما تتوقعه الواجهة (ISO 8601)
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        this.level = level;
        this.source = source;
        this.message = message;
    }

    // Getters
    public String getId() { return id; }
    public String getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getSource() { return source; }
    public String getMessage() { return message; }
}