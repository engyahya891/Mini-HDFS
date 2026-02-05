package com.hdfs.namenode.repository;

import com.hdfs.namenode.model.WorkerNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerRepository extends JpaRepository<WorkerNode, Long> {

    WorkerNode findByUrl(String url);

    List<WorkerNode> findByUrlStartingWith(String prefix);
}