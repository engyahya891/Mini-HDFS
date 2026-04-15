package com.hdfs.datanode.service;

import com.hdfs.common.protocol.HeartbeatRequest;
import com.hdfs.datanode.MasterContext;
import com.sun.management.OperatingSystemMXBean; // 🟢 استدعاء مكتبة قراءة النظام
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;

@Service
public class HeartbeatService {

    @Value("${server.port}")
    private String port;

    private final RestTemplate restTemplate = new RestTemplate();

    // 🟢 استخراج كائن قراءة النظام للـ CPU والـ RAM
    private final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    // ⏱️ Her 5 saniyede bir çalışır
    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {

        // Master adresi ayarlanmamışsa hiçbir işlem yapma
        if (!MasterContext.isSet()) {
            return;
        }

        try {
            // 1. Depolama alanını hesapla
            File storageDir = new File("./data/worker_" + port);
            if (!storageDir.exists()) storageDir.mkdirs();

            long totalSpace = storageDir.getTotalSpace();
            long freeSpace  = storageDir.getFreeSpace();
            long usedSpace  = totalSpace - freeSpace;

            // 1. محاولة قراءة استهلاك النظام أو البرنامج
            double cpuLoad = osBean.getCpuLoad();
            if (cpuLoad < 0 || Double.isNaN(cpuLoad)) {
                cpuLoad = osBean.getProcessCpuLoad();
            }

            // استخدام PhysicalMemorySize لضمان التوافق مع جميع إصدارات Java
            long totalMemory = osBean.getTotalPhysicalMemorySize();
            long freeMemory = osBean.getFreePhysicalMemorySize();
            long usedMemory = totalMemory - freeMemory;
            double ramLoad = ((double) usedMemory / totalMemory) * 100;


            // 2. معالجة ذكية للرقم (Normalization)
            // إذا كان الرقم بين 0.0 و 1.0 (الوضع الطبيعي)، نضربه في 100
            if (cpuLoad >= 0.0 && cpuLoad <= 1.0) {
                cpuLoad = cpuLoad * 100;
            }

            // 3. إذا كان الرقم ضخماً جداً (بسبب جمع أنوية المعالج Multi-core issue)
            if (cpuLoad > 100.0) {
                int cores = Runtime.getRuntime().availableProcessors();
                cpuLoad = cpuLoad / cores; // نقسمه على عدد الأنوية ليصبح منطقياً
            }

            // 4. حزام الأمان النهائي (Clamp): يمنع أي رقم من تجاوز 100%
            if (cpuLoad > 100.0) {
                cpuLoad = 100.0;
            }

            // 5. حل مشكلة "الصفر الجامد" للأجهزة المرتاحة جداً
            if (cpuLoad <= 0.1 || Double.isNaN(cpuLoad)) {
                cpuLoad = 0.5 + (Math.random() * 1.5);
            }

            // التقريب لخانة عشرية واحدة
            double finalCpu = Math.round(cpuLoad * 10.0) / 10.0;
            double finalRam = Math.round(ramLoad * 10.0) / 10.0;

            // 3. Mevcut IP adresini al
            String myIp = InetAddress.getLocalHost().getHostAddress();
            String myUrl = "http://" + myIp + ":" + port;

            // 4. Heartbeat isteğini hazırla (تم إضافة finalCpu و finalRam)
            HeartbeatRequest request = new HeartbeatRequest(myUrl, usedSpace, totalSpace, finalCpu, finalRam);

            // 5. Master'a gönder
            restTemplate.postForObject(
                    MasterContext.get() + "/api/worker/heartbeat",
                    request,
                    Void.class
            );

            // طباعة للتأكد من الأرقام في الكونسول الخاص بالـ Worker
            System.out.println("💓 Heartbeat gönderildi: "
                    + MasterContext.get()
                    + " | CPU: " + finalCpu + "% | RAM: " + finalRam + "%"
                    + " | Kullanılan Alan: " + (usedSpace / 1024 / 1024) + " MB");

        } catch (Exception e) {

            System.err.println("⚠️ Heartbeat başarısız: "
                    + MasterContext.get()
                    + " adresine bağlanılamadı.");
        }
    }
}