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
    private NotificationService notificationService;

    @Autowired
    private LogService logService;

    private long previousActiveWorkerCount = -1; // Track previous state

    // Check worker health every 10 seconds
    @Scheduled(fixedRate = 10000)
    public void checkWorkerHealth() {
        List<WorkerNode> allWorkers = workerRepository.findAll();
        // If no heartbeat for 15 seconds, consider dead
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(15);

        boolean workerDiedInThisCycle = false;

        for (WorkerNode worker : allWorkers) {
            if (worker.isActive() && worker.getLastHeartbeat().isBefore(threshold)) {

                worker.setActive(false); // Mark as dead
                workerRepository.save(worker);
                workerDiedInThisCycle = true;

                System.out.println("🔴 Düğüm bağlantısı koptu: " + worker.getUrl());

                logService.addLog("ERROR", "HealthMonitor",
                        worker.getUrl() + " IP adresli düğüm yanıt vermiyor (Offline)!");

                notificationService.addNotification(
                        "error",
                        "Bağlantı Kesildi",
                        worker.getUrl() + " IP adresli düğümle bağlantı koptu (Çevrimdışı).");
            }
        }

        // حساب عدد الخوادم النشطة الحالي
        long currentActive = allWorkers.stream().filter(WorkerNode::isActive).count();

        // طباعة توضيحية عند سقوط أي خادم لمعرفة العدد الفعلي المتبقي
        if (workerDiedInThisCycle) {
            System.out.println("📉 [System Monitor] Aktif Worker sayısı güncellendi. Kalan: " + currentActive);
        }

        // خوارزمية تتبع حالة النظام بناءً على التحولات (State Transitions)
        if (previousActiveWorkerCount != -1) {
            
            // 1. حالة التعافي: من أقل من 2 إلى 2 أو أكثر
            if (previousActiveWorkerCount < 2 && currentActive >= 2) {
                System.out.println("\n✅ SİSTEM İYİLEŞTİ: Sistemde en az 2 aktif Worker var. Replikasyon devrede! ✅\n");
                logService.addLog("INFO", "SystemMonitor", 
                        "Sistem İyileşti: Yeterli düğüm sayısına (" + currentActive + " Worker) ulaşıldı. Hata toleransı aktif.");
                notificationService.addNotification("success", "Sistem İyileşti", 
                        "Sistemdeki aktif düğüm sayısı " + currentActive + " oldu. Veri yedekliliği devrede.");
            }
            
            // 2. حالة التحذير: هبوط العدد من 2 (أو أكثر) إلى 1 فقط
            else if (previousActiveWorkerCount >= 2 && currentActive == 1) {
                System.out.println("\n⚠️ DİKKAT: Sistemde SADECE 1 aktif Worker kaldı! Replikasyon yapılamaz! ⚠️\n");
                logService.addLog("ERROR", "SystemMonitor",
                        "Sistemde sadece 1 aktif düğüm (Worker) kaldı. Hata toleransı (Fault Tolerance) devre dışı!");
                notificationService.addNotification("warning", "Kritik Seviye!",
                        "Sadece 1 düğüm çalışıyor. Veri yedekliliği sağlanamıyor.");
            }
            
            // 3. الحالة الحرجة: هبوط العدد من 1 (أو أكثر) إلى 0
            else if (previousActiveWorkerCount >= 1 && currentActive == 0) {
                System.out.println("\n🚨 KRİTİK ALARM: Sistemde çalışan HİÇBİR Worker kalmadı! Tüm sistem durdu! 🚨\n");
                logService.addLog("CRITICAL", "SystemMonitor",
                        "Sistemde çalışan HİÇBİR düğüm (Worker) kalmadı! Veri kaybı riski yüksek!");
                notificationService.addNotification("error", "SİSTEM ÇÖKTÜ!",
                        "Tüm düğümler çevrimdışı. Lütfen acilen sunucuları kontrol edin.");
            }
            
        } else {
            // في حالة التشغيل لأول مرة (previous == -1)، إذا كان العدد ضعيفاً نعطيه تنبيه
            if (currentActive == 1) {
                System.out.println("\n⚠️ DİKKAT: Sistemde SADECE 1 aktif Worker var! Replikasyon yapılamaz! ⚠️\n");
            } else if (currentActive == 0) {
                System.out.println("\n🚨 KRİTİK ALARM: Sistemde çalışan HİÇBİR Worker yok! 🚨\n");
            }
        }

        // تحديث العدد السابق للدورة القادمة
        previousActiveWorkerCount = currentActive;
    }
}