package com.hdfs.datanode.service;

import com.hdfs.common.protocol.StorageReportRequest;
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

    // عنوان الماستر
    private String masterUrl = "http://localhost:8080";

    private final RestTemplate restTemplate = new RestTemplate();


    public void setMasterUrl(String masterUrl) {
        this.masterUrl = masterUrl;
    }
    // ⏱️ يعمل كل 10 ثوانٍ لإرسال قائمة الملفات
    @Scheduled(fixedRate = 10000)
    public void sendBlockReport() {
        try {
            // 1. تحديد المجلد
            String storagePath = "./data/worker_" + port;
            File dir = new File(storagePath);
            if (!dir.exists()) return;

            // 2. قراءة أسماء الملفات (البلوكات)
            File[] files = dir.listFiles();
            List<String> blockIds = new ArrayList<>();
            long used = 0;

            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        blockIds.add(f.getName()); // اسم الملف (مثلاً: video.mp4_part_1)
                        used += f.length();
                    }
                }
            }

            // 3. تحديد عنواني (نفس منطق Heartbeat لكي يطابق المسجل في الماستر)
            String myIp = InetAddress.getLocalHost().getHostAddress();
            String myUrl = "http://" + myIp + ":" + port;

            // 4. تجهيز التقرير
            StorageReportRequest request = new StorageReportRequest();
            request.setWorkerUrl(myUrl); // مهم جداً أن يطابق عنوان الوركر المسجل
            request.setBlockIds(blockIds); // 👈 هذه هي القائمة التي ستملأ جدول BLOCKS
            request.setUsed(used);
            request.setCapacity(dir.getTotalSpace());

            // 5. الإرسال للماستر
            restTemplate.postForObject(masterUrl + "/api/worker/report", request, String.class);

            System.out.println("📦 Blok Raporu Gönderildi: " + blockIds.size() + " blok bulundu.");

        } catch (Exception e) {
            System.err.println("⚠️ Blok Raporu Başarısız: " + e.getMessage());
        }
    }
}