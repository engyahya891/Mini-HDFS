package com.hdfs.datanode.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worker")
public class WorkerAdminController {

    // 🟢 دالة الإغلاق الذاتي (Self-Destruct)
    @PostMapping("/shutdown")
    public ResponseEntity<String> shutdown() {
        System.out.println("\n🛑 DİKKAT: Master'dan KAPANMA (Shutdown) emri alındı!");
        System.out.println("☠️ Worker sunucusu derhal kapatılıyor...");

        // نقوم بتشغيل الإغلاق في Thread منفصل لكي نتمكن من إرسال رد (OK) للماستر قبل أن نموت
        new Thread(() -> {
            try {
                Thread.sleep(1000); // ننتظر ثانية واحدة فقط
                System.exit(0); // إغلاق برنامج الووركر بالكامل!
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        return ResponseEntity.ok("Worker kapatılıyor...");
    }
}