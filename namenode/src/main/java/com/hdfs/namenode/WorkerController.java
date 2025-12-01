package com.hdfs.namenode;

import com.hdfs.common.protocol.WorkerRegisterRequest; // استيراد الكلاس المشترك
import org.springframework.web.bind.annotation.*;

@RestController // يخبر Spring أن هذا الكلاس مخصص لاستقبال طلبات الويب
@RequestMapping("/api/worker")
public class WorkerController {

    @PostMapping("/register")
    public void registerWorker(@RequestBody WorkerRegisterRequest request) {
        // هنا يستقبل الماستر البيانات
        System.out.println("🔔 Yeni Kayıt Talebi Alındı!");
        System.out.println("   - Port: " + request.getPort());
        System.out.println("   - Path: " + request.getStoragePath());

        // لاحقاً سنضيف كود لحفظ هذا الووركر في قائمة
    }
}