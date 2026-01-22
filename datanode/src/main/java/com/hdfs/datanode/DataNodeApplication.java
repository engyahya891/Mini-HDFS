package com.hdfs.datanode;

import com.hdfs.common.protocol.WorkerRegisterRequest; // استيراد الكلاس المشترك
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class DataNodeApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DataNodeApplication.class, args);
    }

    // هذه الدالة تعمل تلقائياً فور تشغيل السيرفر
    @Override
    public void run(String... args) throws Exception {
        System.out.println("İşçi başladı! Master'a kaydolmaya çalışılıyor...");

        // 1. تجهيز البيانات (باستخدام الكلاس المشترك)
        WorkerRegisterRequest request = new WorkerRegisterRequest();
        request.setPort(8081); // سنفترض أن هذا الووركر يعمل على 8081
        request.setStoragePath("/tmp/worker1");

        // 2. إرسال الطلب إلى الماستر
        RestTemplate restTemplate = new RestTemplate();
        String masterUrl = "http://192.168.1.7:8080/api/worker/register";

        try {
            // إرسال POST Request
            restTemplate.postForObject(masterUrl, request, Void.class);
            System.out.println("✅ Kayıt başarılı!");
        } catch (Exception e) {
            System.out.println("❌ Master'a bağlanılamadı: " + e.getMessage());
        }
    }
}