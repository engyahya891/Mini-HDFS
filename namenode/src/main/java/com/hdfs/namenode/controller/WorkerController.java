package com.hdfs.namenode.controller;

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
    public ResponseEntity<String> registerWorker(@RequestBody WorkerRegisterRequest request) {

        // 1. طباعة التفاصيل (من كودك القديم) - Loglama
        System.out.println("🔔 Yeni Kayıt Talebi Alındı! (New Worker Request)");
        System.out.println("   - Port: " + request.getPort());
        System.out.println("   - Path: " + request.getStoragePath());

        // 2. المنطق الجديد: إنشاء الرابط والحفظ في قاعدة البيانات
        // ملاحظة: نفترض حالياً أنهم على نفس الجهاز (localhost)
        String workerUrl = "http://localhost:" + request.getPort();

        // التحقق: هل هذا الوركر موجود مسبقاً؟
        WorkerNode existingWorker = workerRepository.findByUrl(workerUrl);

        if (existingWorker != null) {
            System.out.println("ℹ️ Bu worker zaten kayıtlı. (Worker already registered)");
            existingWorker.setActive(true);
            workerRepository.save(existingWorker);
            return ResponseEntity.ok("Welcome back! You are already registered.");
        }

        // 3. الحفظ لأول مرة (New Registration)
        WorkerNode newWorker = new WorkerNode(workerUrl);
        workerRepository.save(newWorker);

        System.out.println("✅ Kayıt Başarılı: " + workerUrl);
        return ResponseEntity.ok("Registration Successful!");
    }
}