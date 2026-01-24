package com.hdfs.namenode.repository;

import com.hdfs.namenode.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, String> {

    // 🟢 هذا هو السطر الناقص!
    // نحن نخبر Spring: "قم بإنشاء استعلام يبحث في عمود filename"
    FileMetadata findByFilename(String filename);
}