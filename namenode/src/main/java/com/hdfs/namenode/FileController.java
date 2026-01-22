package com.hdfs.namenode;

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

    // دالة الرفع (Upload)
    @PostMapping("/upload")
    public ClientUploadResponse handleUploadRequest(@RequestBody ClientUploadRequest request) {
        System.out.println("📥 Dosya yükleme isteği: " + request.getFilename());

        String targetWorkerUrl = "http://localhost:8081";

        FileMetadata metadata = new FileMetadata(request.getFilename(), targetWorkerUrl, request.getFileSize());
        fileRepository.save(metadata);

        return new ClientUploadResponse(true, targetWorkerUrl);
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
    public String deleteFile(@PathVariable String filename) {
        System.out.println("🗑️ silme isteği: " + filename);

        // 1. البحث عن الملف في قاعدة البيانات
        Optional<FileMetadata> fileData = fileRepository.findById(filename);

        if (fileData.isEmpty()) {
            return "ERROR: File not found in Database";
        }

        String workerUrl = fileData.get().getWorkerUrl(); // عنوان الووركر (مثلاً http://192.168.1.20:8081)

        try {
            // 2. أمر الووركر بحذف الملف الفعلي
            // نرسل طلب DELETE إلى الووركر
            restTemplate.delete(workerUrl + "/api/data/delete/" + filename);

            // 3. إذا لم يحدث خطأ في السطر السابق، نحذف من قاعدة البيانات
            fileRepository.deleteById(filename);

            System.out.println("✅ Veritabanından ve diskten silindi.");
            return "SUCCESS: File deleted";

        } catch (Exception e) {
            System.out.println("❌ Worker’dan dosya silme işlemi başarısız oldu " + e.getMessage());
            return "ERROR: Could not delete file from Worker";
        }
    }
}