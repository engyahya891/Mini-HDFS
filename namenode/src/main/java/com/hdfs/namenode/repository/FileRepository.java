package com.hdfs.namenode.repository;

import com.hdfs.namenode.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; // 🟢 مهم جداً: أضفنا هذا الاستيراد للقوائم

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, String> {

    // الدالة القديمة (تبحث عن ملف واحد بالاسم)
    FileMetadata findByFilename(String filename);

    // 🟢 الدالة الجديدة: (تبحث عن كل الملفات المملوكة لشخص محدد)
    // Spring سيقوم بترجمة هذا السطر تلقائياً إلى: SELECT * FROM files WHERE owner = ?
    List<FileMetadata> findByOwner(String owner);
}