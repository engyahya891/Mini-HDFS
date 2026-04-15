package com.hdfs.namenode.service;

/*

هذه الخدمة هي "الذاكرة" التي ستحتفظ بالسجلات. سنجعلها تحتفظ بآخر 200 سجل فقط لكي لا تمتلئ ذاكرة (RAM) السيرفر بمرور الوقت.


 */
import com.hdfs.namenode.model.SystemLog;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class LogService {

    // استخدام LinkedList لأننا سنضيف ونحذف من الأطراف باستمرار
    private final LinkedList<SystemLog> logs = new LinkedList<>();
    private static final int MAX_LOGS = 200; // الاحتفاظ بآخر 200 حدث فقط

    // دالة لإضافة سجل جديد
    public synchronized void addLog(String level, String source, String message) {
        SystemLog newLog = new SystemLog(level, source, message);

        // إضافة في البداية ليكون الأحدث في الأعلى
        logs.addFirst(newLog);

        // إزالة الأقدم إذا تجاوزنا الحد المسموح
        if (logs.size() > MAX_LOGS) {
            logs.removeLast();
        }
    }

    // دالة لجلب كل السجلات
    public synchronized List<SystemLog> getAllLogs() {
        return logs;
    }
}