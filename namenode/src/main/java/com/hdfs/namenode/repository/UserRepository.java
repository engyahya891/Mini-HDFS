package com.hdfs.namenode.repository;

import com.hdfs.namenode.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // دالة للبحث عن المستخدم بواسطة اسمه
    User findByUsername(String username);
}