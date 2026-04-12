package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.Notification;
import com.hdfs.namenode.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*") // للسماح لـ React بالوصول للبيانات
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // جلب جميع الإشعارات
    @GetMapping
    public List<Notification> getNotifications() {
        return notificationService.getAll();
    }

    // (اختياري) دالة لإضافة إشعار تجريبي لاختبار النظام من المتصفح
    @GetMapping("/test")
    public String addTestNotification() {
        notificationService.addNotification(
                "success",
                "Test Başarılı",
                "Bildirim sistemi başarıyla bağlandı!"
        );
        return "Test bildirimi eklendi.";
    }
}