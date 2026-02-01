package com.hdfs.namenode.repository;

import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.WorkerNode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockRepository extends JpaRepository<BlockMetadata, Long> {

    void deleteByWorker(WorkerNode worker);
    long countByWorker(WorkerNode worker);


}
