package com.hdfs.namenode.repository;

import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.WorkerNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BlockRepository extends JpaRepository<BlockMetadata, Long> {

    void deleteByWorker(WorkerNode worker);
    long countByWorker(WorkerNode worker);

    // 🟢 1. البحث عن كل البلوكات التي كانت في الـ Worker الميت (ماذا خسرنا؟)
    List<BlockMetadata> findByWorker(WorkerNode worker);

    // 🟢 2. البحث عن كل النسخ المتاحة لبلوك معين (من هو الناجي؟)
    List<BlockMetadata> findByBlockId(String blockId);
}