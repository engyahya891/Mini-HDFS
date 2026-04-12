package com.hdfs.namenode.service;

import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkerHealthMonitor {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private NotificationService notificationService; // 🟢 حقن خدمة الإشعارات

    // يفحص صحة العمال كل 10 ثوانٍ
    @Scheduled(fixedRate = 10000)
    public void checkWorkerHealth() {
        List<WorkerNode> activeWorkers = workerRepository.findAll();
        // إذا مرت 15 ثانية دون أن يرسل العامل نبضة، نعتبره ميتاً
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(15);

        for (WorkerNode worker : activeWorkers) {
            if (worker.isActive() && worker.getLastHeartbeat().isBefore(threshold)) {

                worker.setActive(false); // تحويله إلى ميت في قاعدة البيانات
                workerRepository.save(worker);

                System.out.println("🔴 Düğüm bağlantısı koptu: " + worker.getUrl());

                // 🔴 إطلاق إشعار الانقطاع للواجهة!
                notificationService.addNotification(
                        "error",
                        "Bağlantı Kesildi",
                        worker.getUrl() + " IP adresli düğümle bağlantı koptu (Çevrimdışı)."
                );
            }
        }
    }
}