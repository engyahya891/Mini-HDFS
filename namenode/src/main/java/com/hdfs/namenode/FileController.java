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
}