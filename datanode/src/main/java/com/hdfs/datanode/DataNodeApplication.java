package com.hdfs.datanode;

import com.hdfs.common.protocol.WorkerRegisterRequest;
import org.springframework.beans.factory.annotation.Value; // استيراد مهم
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@SpringBootApplication
public class DataNodeApplication implements CommandLineRunner {

    // 🟢 1. نقرأ البورت الذي تم تشغيل التطبيق عليه
    @Value("${server.port}")
    private int serverPort;

    public static void main(String[] args) {
        SpringApplication.run(DataNodeApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Worker Başlatıldı (Port: " + serverPort + ")");

        // 🟢 2. جعل مسار التخزين ديناميكياً بناءً على البورت
        // إذا كان البورت 8081 سيكون المجلد /tmp/hdfs-data-8081
        String dynamicStoragePath = "./data/worker_" + serverPort;

        // إنشاء المجلد إذا لم يكن موجوداً
        new File(dynamicStoragePath).mkdirs();

        System.out.println("📂 Depolama yolu: " + dynamicStoragePath);

        // 3. تجهيز طلب التسجيل
        WorkerRegisterRequest request = new WorkerRegisterRequest();
        request.setPort(serverPort); // نستخدم البورت الحقيقي
        request.setStoragePath(dynamicStoragePath); // نرسل المسار الصحيح

        // 4. إرسال الطلب إلى الماستر
        RestTemplate restTemplate = new RestTemplate();
        // تأكد من كتابة localhost بحروف صغيرة، ومن أن الماستر يعمل على 8080
        String masterUrl = "http://localhost:8080/api/worker/register";

        try {
            System.out.println("📡 Master'a bağlanılıyor...");
            // الجديد: استقبل الرد كنص String حتى لو لم تستخدمه
            restTemplate.postForObject(masterUrl, request, String.class);
            System.out.println("✅ MASTER ONAYLADI: Bu worker sisteme eklendi!");
        } catch (Exception e) {
            System.out.println("❌ HATA: Master'a ulaşılamadı. (Master çalışıyor mu?)");
            // في الواقع العملي، هنا نضع حلقة تكرار (Retry Logic) للمحاولة كل 5 ثواني
        }
    }
}