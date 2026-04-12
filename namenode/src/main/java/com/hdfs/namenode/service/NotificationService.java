package com.hdfs.namenode.service;

import com.hdfs.namenode.model.Notification;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

// ستحتاج إلى استدعاء كلاس Notification إذا وضعته في مجلد آخر (مثل مجلد model)
// import com.yourproject.namenode.model.Notification;

@Service
public class NotificationService {

    private final List<Notification> notifications = new LinkedList<>();

    public void addNotification(String type, String title, String message) {
        Notification n = new Notification();
        n.setId(UUID.randomUUID().toString());
        n.setType(type); // success, error, warning, info
        n.setTitle(title);
        n.setMessage(message);
        n.setTimestamp(new SimpleDateFormat("HH:mm:ss").format(new Date()));
        n.setRead(false);

        // إضافة الإشعار الجديد في بداية القائمة (لكي يظهر أولاً)
        notifications.add(0, n);

        // الاحتفاظ بآخر 20 إشعار فقط لمنع امتلاء الذاكرة
        if (notifications.size() > 20) {
            notifications.remove(notifications.size() - 1);
        }
    }

    public List<Notification> getAll() {
        return notifications;
    }
}