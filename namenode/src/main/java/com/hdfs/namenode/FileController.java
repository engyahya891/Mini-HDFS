package com.hdfs.namenode;

import org.springframework.http.ResponseEntity;
import com.hdfs.common.protocol.ClientUploadRequest;
import com.hdfs.common.protocol.ClientUploadResponse;
import com.hdfs.namenode.model.FileMetadata;
import com.hdfs.namenode.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.Optional;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileRepository fileRepository;



    // دالة الرفع (Upload) المعدلة
    @PostMapping("/upload")
    public ResponseEntity<ClientUploadResponse> handleUploadRequest(@RequestBody ClientUploadRequest request) {

        System.out.println("📥 Dosya yükleme isteği: " + request.getFilename());

        // 1️⃣ الخطوة الجديدة: التحقق من وجود الملف
        // نبحث عنه في القاعدة، إذا وجدناه نوقف العملية فوراً
        if (fileRepository.findByFilename(request.getFilename()) != null) {
            System.out.println("⚠️ Yinelenen dosya tespit edildi: " + request.getFilename());
            // نرجع كود 409 Conflict
            return ResponseEntity.status(409).build();
        }

        // 2️⃣ إذا لم يكن موجوداً، نكمل العمل الطبيعي
        String targetWorkerUrl = "http://localhost:8081";

        // نستخدم الكلاس الخاص بك FileMetadata كما هو
        FileMetadata metadata = new FileMetadata(request.getFilename(), targetWorkerUrl, request.getFileSize());
        fileRepository.save(metadata);

        // نرجع كود 200 OK مع الاستجابة
        return ResponseEntity.ok(new ClientUploadResponse(true, targetWorkerUrl));
    }

    // دالة البحث
    @GetMapping("/locate/{filename}")
    public String locateFile(@PathVariable String filename) {
        System.out.println("🔎 Searching for file: " + filename);

        Optional<FileMetadata> fileData = fileRepository.findById(filename);

        if (fileData.isPresent()) {
            return fileData.get().getWorkerUrl();
        } else {
            return "NOT_FOUND";
        }
    }

    // نحتاج لـ RestTemplate للاتصال بالووركر
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) { // لاحظ: القوس مفتوح هنا فقط

        System.out.println("🗑️ silme isteği " + filename);

        // 1. هذا السطر كان ناقصاً عندك: يجب جلب بيانات الملف أولاً
        Optional<FileMetadata> fileData = fileRepository.findById(filename);

        // التحقق مما إذا كان الملف موجوداً أصلاً
        if (fileData.isEmpty()) {
            return ResponseEntity.status(404).body("❌ Dosya sistemde mevcut değil.");
        }

        // الآن يمكننا استخدام fileData بأمان
        String workerUrl = fileData.get().getWorkerUrl();

        try {
            // 2. محاولة الاتصال بالووركر
            restTemplate.delete(workerUrl + "/api/data/delete/" + filename);

            // 3. إذا وصلنا لهنا، يعني الووركر رد بـ OK، نحذف من الداتا بيس
            fileRepository.deleteById(filename);
            return ResponseEntity.ok("✅ Veritabanından ve diskten silindi.");

        } catch (Exception e) {
            // 4. هنا نلتقط خطأ الـ Timeout أو اختلاف الـ IP
            System.out.println("❌ Worker’a bağlanma başarısız oldu: " + e.getMessage());

            // نرجع 500 ليظهر للعميل أن هناك مشكلة في الشبكة
            return ResponseEntity.status(500).body("❌ Worker’a bağlanılamadı: IP adresinin doğru ve aktif olduğundan emin olun.");
        }
    } // إغلاق الدالة هنا في النهاية
}
