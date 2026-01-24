package com.hdfs.namenode.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.hdfs.common.protocol.WorkerRegisterRequest;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    @Autowired
    private WorkerRepository workerRepository;

    @PostMapping("/register")
    public ResponseEntity<String> registerWorker(@RequestBody WorkerRegisterRequest request, HttpServletRequest servletRequest) {

        // 1. الحصول على IP الجهاز المتصل بشكل ديناميكي
        String ipAddress = servletRequest.getRemoteAddr();

        // تنظيف بسيط: إذا كان IP محلي بصيغة IPv6، نحوله لـ IPv4 ليسهل قراءته
        // (هذا السطر فقط للجماليات، ولا يؤثر على العمل مع الأجهزة الخارجية)
        if (ipAddress.equals("0:0:0:0:0:0:0:1")) {
            ipAddress = "127.0.0.1";
        }

        System.out.println("🔔 Yeni Kayıt Talebi (New Connection): " + ipAddress);

        // 2. تكوين الرابط: نستخدم الـ IP الذي وصلنا منه الطلب + البورت الذي أرسله الوركر
        // هنا السر! نحن لا نكتب localhost بيدينا، بل نستخدم ipAddress المتغير
        String workerUrl = "http://" + ipAddress + ":" + request.getPort();

        // التحقق والحفظ (كما هو سابقاً)
        WorkerNode existingWorker = workerRepository.findByUrl(workerUrl);

        if (existingWorker != null) {
            System.out.println("ℹ️ Bu worker zaten kayıtlı: " + workerUrl);
            existingWorker.setActive(true);
            workerRepository.save(existingWorker);
            return ResponseEntity.ok("Welcome back! You are already registered.");
        }

        // الحفظ الجديد
        WorkerNode newWorker = new WorkerNode(workerUrl);
        workerRepository.save(newWorker);

        System.out.println("✅ Kayıt Başarılı (Registered): " + workerUrl);
        return ResponseEntity.ok("Registration Successful!");
    }
}