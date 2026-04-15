/*
*
*
*هذا هو المسار (Endpoint) الذي ستتصل به واجهة React http://localhost:8080/api/logs لجلب البيانات كل 10 ثوانٍ.
*
* */



package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.SystemLog;
import com.hdfs.namenode.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin("*") // مهم جداً لكي يسمح لـ React بالاتصال
public class LogController {

    @Autowired
    private LogService logService;

    // React سيقوم بعمل GET لهذا المسار
    @GetMapping
    public ResponseEntity<List<SystemLog>> getLogs() {
        return ResponseEntity.ok(logService.getAllLogs());
    }
}
