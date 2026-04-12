package com.hdfs.namenode.controller;

import com.hdfs.namenode.service.SystemConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    @Autowired
    private SystemConfigManager configManager;

    @GetMapping
    public ResponseEntity<?> getCurrentConfig() {
        return ResponseEntity.ok(Map.of(
                "blockSize", configManager.getBlockSizeMB(),
                "replication", configManager.getReplicationFactor()
        ));
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Object> request) {
        String inputPassword = (String) request.get("adminPassword");

        if (!configManager.getAdminPassword().equals(inputPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Hatalı yönetici şifresi!");
        }

        Map<String, Integer> newConfig = (Map<String, Integer>) request.get("config");
        configManager.setBlockSizeMB(newConfig.get("blockSize"));
        configManager.setReplicationFactor(newConfig.get("replication"));

        System.out.println("✅ Konfigürasyon güncellendi! Block: " + configManager.getBlockSizeMB() + "MB, Rep: " + configManager.getReplicationFactor());

        return ResponseEntity.ok("Sistem konfigürasyonu başarıyla güncellendi!");
    }
}