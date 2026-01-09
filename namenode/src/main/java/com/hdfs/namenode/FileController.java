package com.hdfs.namenode;

import com.hdfs.common.protocol.ClientUploadRequest;
import com.hdfs.common.protocol.ClientUploadResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file") // أي رابط يبدأ بهذا العنوان سيأتي هنا
public class FileController {

    @PostMapping("/upload")
    public ClientUploadResponse handleUploadRequest(@RequestBody ClientUploadRequest request) {
        // شرح: الماستر يستقبل طلب الرفع هنا
        System.out.println("📥 Dosya yükleme isteği alındı: " + request.getFilename()); // (Log للادمن)

        // المنطق البسيط حالياً: سنوجه العميل دائماً إلى الووركر رقم 1
        // في المستقبل، سنقوم هنا باختيار الووركر الأقل ازدحاماً
        String targetWorkerUrl = "http://localhost:8081";

        // تجهيز الرد
        return new ClientUploadResponse(true, targetWorkerUrl);
    }
}