package com.hdfs.namenode;

import org.springframework.http.ResponseEntity;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import com.hdfs.common.protocol.ClientUploadRequest;
import com.hdfs.common.protocol.ClientUploadResponse;
import com.hdfs.namenode.model.FileMetadata;
import com.hdfs.namenode.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileRepository fileRepository;

    // 🟢 1. نحتاج هذا المتغير للوصول لجدول الووركرز
    @Autowired
    private WorkerRepository workerRepository;

    // 🟢 2. متغير بسيط لتطبيق خوارزمية الدور (Round Robin)
    private int currentWorkerIndex = 0;

    // --- دالة الرفع المعدلة (Upload) ---
    @PostMapping("/upload")
    public ResponseEntity<ClientUploadResponse> handleUploadRequest(@RequestBody ClientUploadRequest request) {

        System.out.println("📥 Dosya yükleme isteği: " + request.getFilename());

        // أ) التحقق من التكرار (كما هو، ممتاز)
        if (fileRepository.findByFilename(request.getFilename()) != null) {
            System.out.println("⚠️ Yinelenen dosya tespit edildi (Duplicate): " + request.getFilename());
            return ResponseEntity.status(409).build();
        }

        List<WorkerNode> workers = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive) // شرط: يجب أن يكون نشطاً
                .toList();

        if (workers.isEmpty()) {
            System.out.println("❌ Kritik Hata: Hiçbir aktif worker yok!");
            return ResponseEntity.status(500).build();
        }

        // خوارزمية الدور (Round Robin)
        // نأخذ الرقم الحالي % عدد السيرفرات (لضمان أننا لا نخرج عن حدود القائمة)
        WorkerNode selectedWorker = workers.get(currentWorkerIndex % workers.size());

        // نزيد العداد للمرة القادمة
        currentWorkerIndex++;

        String targetWorkerUrl = selectedWorker.getUrl();
        System.out.println("🎯 Seçilen Worker: " + targetWorkerUrl); // طباعة لمعرفة من تم اختياره

        // ج) الحفظ في القاعدة
        FileMetadata metadata = new FileMetadata(request.getFilename(), targetWorkerUrl, request.getFileSize());
        fileRepository.save(metadata);

        // د) الرد على العميل
        return ResponseEntity.ok(new ClientUploadResponse(true, targetWorkerUrl));
    }

    // --- دالة البحث (Locate) ---
    // (لم تتغير كثيراً، لكن تأكدنا أنها ترجع الرابط المحفوظ)
    @GetMapping("/locate/{filename}")
    public ResponseEntity<String> locateFile(@PathVariable String filename) {
        System.out.println("🔎 Searching for file: " + filename);

        // نستخدم findByFilename لأنها الأدق
        FileMetadata fileData = fileRepository.findByFilename(filename);

        if (fileData != null) {
            return ResponseEntity.ok(fileData.getWorkerUrl());
        } else {
            return ResponseEntity.status(404).body("NOT_FOUND");
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
