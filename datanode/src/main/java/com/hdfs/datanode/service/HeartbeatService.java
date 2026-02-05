package com.hdfs.datanode.service;

import com.hdfs.common.protocol.HeartbeatRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.net.InetAddress;

@Service
public class HeartbeatService {

    @Value("${server.port}")
    private String port;

    // القيمة الافتراضية (سيتم تغييرها إذا أدخلت IP آخر في البداية)
    private String masterUrl = "http://localhost:8080";

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ هذه الدالة صحيحة 100%، وهي التي تسمح بتغيير الرابط
    public void setMasterUrl(String masterUrl) {
        this.masterUrl = masterUrl;
        System.out.println("💓 Heartbeat Service target updated to: " + masterUrl);
    }

    // ⏱️ يعمل كل 5 ثوانٍ
    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        try {
            // 1. حساب المساحة
            File storageDir = new File("./data/worker_" + port);
            if (!storageDir.exists()) storageDir.mkdirs();

            long totalSpace = storageDir.getTotalSpace();
            long freeSpace  = storageDir.getFreeSpace();
            long usedSpace  = totalSpace - freeSpace;

            // 2. معرفة IP جهازي الحالي
            String myIp = InetAddress.getLocalHost().getHostAddress();
            String myUrl = "http://" + myIp + ":" + port;

            // 3. تجهيز التقرير
            HeartbeatRequest request = new HeartbeatRequest(myUrl, usedSpace, totalSpace);

            // 4. الإرسال (سيستخدم masterUrl المحدث)
            restTemplate.postForObject(
                    masterUrl + "/api/worker/heartbeat",
                    request,
                    Void.class
            );

            System.out.println("💓 Heartbeat sent to " + masterUrl + " | Used: " + (usedSpace / 1024 / 1024) + " MB");

        } catch (Exception e) {
            // رسالة خطأ مختصرة لتجنب إزعاجك إذا كان الماستر مغلقاً
            System.err.println("⚠️ Heartbeat failed: Connect to " + masterUrl + " failed.");
        }
    }
}