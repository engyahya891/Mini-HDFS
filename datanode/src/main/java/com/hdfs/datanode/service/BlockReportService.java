package com.hdfs.datanode.service;

import com.hdfs.common.protocol.StorageReportRequest;
import com.hdfs.datanode.MasterContext; // 👈 استدعاء الكلاس
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Service
public class BlockReportService {

    @Value("${server.port}")
    private String port;

    private final RestTemplate restTemplate = new RestTemplate();

    // ⏱️ يعمل كل 10 ثوانٍ
    @Scheduled(fixedRate = 10000)
    public void sendBlockReport() {
        // 🛑 التعديل الأهم: فحص هل تم إدخال الـ IP أم لا؟
        // إذا لم يتم الإدخال، توقف فوراً ولا ترسل شيئاً (يمنع ظهور الأخطاء)
        if (!MasterContext.isSet()) {
            return;
        }

        try {
            String storagePath = "./data/worker_" + port;
            File dir = new File(storagePath);
            if (!dir.exists()) return;

            File[] files = dir.listFiles();
            List<String> blockIds = new ArrayList<>();
            long used = 0;

            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        blockIds.add(f.getName());
                        used += f.length();
                    }
                }
            }

            String myIp = InetAddress.getLocalHost().getHostAddress();
            String myUrl = "http://" + myIp + ":" + port;

            StorageReportRequest request = new StorageReportRequest();
            request.setWorkerUrl(myUrl);
            request.setBlockIds(blockIds);
            request.setUsed(used);
            request.setCapacity(dir.getTotalSpace());

            // 👈 استخدام العنوان من MasterContext بدلاً من المتغير المحلي
            restTemplate.postForObject(MasterContext.get() + "/api/worker/report", request, String.class);

            System.out.println("📦 Blok Raporu Gönderildi (" + blockIds.size() + " dosya).");

        } catch (Exception e) {
            // لن تظهر هذه الرسالة إلا إذا أدخلت IP والشبكة فيها مشكلة
            System.err.println("⚠️ Blok Raporu Hatası: " + e.getMessage());
        }
    }
}