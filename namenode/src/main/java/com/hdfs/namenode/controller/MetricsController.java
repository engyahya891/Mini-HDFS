package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*") // مهم جداً للواجهة
public class MetricsController {

    @Autowired
    private WorkerRepository workerRepository;

    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getClusterPerformance() {
        // جلب العمال النشطين فقط (الذين أرسلوا نبضة خلال آخر 15 ثانية)
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(15);
        List<WorkerNode> activeWorkers = workerRepository.findAll().stream()
                .filter(w -> w.getLastHeartbeat() != null && w.getLastHeartbeat().isAfter(threshold))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();

        if (activeWorkers.isEmpty()) {
            response.put("clusterAvgCpu", 0.0);
            response.put("clusterAvgRam", 0.0);
            response.put("workers", activeWorkers);
            return ResponseEntity.ok(response);
        }

        // 🟢 حساب المتوسط العام للكلاستر (للقسم العلوي في الواجهة)
        double totalCpu = 0;
        double totalRam = 0;

        for (WorkerNode worker : activeWorkers) {
            totalCpu += worker.getCpuUsage();
            totalRam += worker.getRamUsage();
        }

        double avgCpu = Math.round((totalCpu / activeWorkers.size()) * 10.0) / 10.0;
        double avgRam = Math.round((totalRam / activeWorkers.size()) * 10.0) / 10.0;

        response.put("clusterAvgCpu", avgCpu);
        response.put("clusterAvgRam", avgRam);

        // 🟢 إرسال بيانات كل عامل على حدة (للقسم السفلي في الواجهة)
        response.put("workers", activeWorkers);

        return ResponseEntity.ok(response);
    }
}