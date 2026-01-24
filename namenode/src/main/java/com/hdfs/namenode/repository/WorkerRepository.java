package com.hdfs.namenode.repository;

import com.hdfs.namenode.model.WorkerNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerRepository extends JpaRepository<WorkerNode, Long> {
    // دالة للبحث عن وركر عن طريق الرابط (لمنع التكرار عند الإضافة)
    WorkerNode findByUrl(String url);
}