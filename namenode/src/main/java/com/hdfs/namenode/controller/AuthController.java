package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.User;
import com.hdfs.namenode.repository.UserRepository;
import com.hdfs.namenode.service.SystemConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // 🟢 مهم لكي لا يظهر خطأ CORS في React
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // 🟢 1. حقن مدير الإعدادات هنا لكي نصل لكلمة مرور المدير
    @Autowired
    private SystemConfigManager configManager;

    // 🟢 تسجيل مستخدم جديد (Register)
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username, @RequestParam String password) {
        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.status(409).body("HATA: Bu kullanıcı adı zaten mevcut!");
        }

        User newUser = new User(username, password);
        userRepository.save(newUser);
        return ResponseEntity.ok("Kayıt başarılı! Artık giriş yapabilirsiniz.");
    }

    // 🟢 تسجيل الدخول (Login) - يدعم العميل والمدير معاً
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {

        // --- 🟢 التحقق من دخول المدير (Admin Dashboard) ---
        if ("admin".equals(username) || "root".equals(username)) {
            // استخدام configManager بدلاً من getInstance()
            String adminPassword = configManager.getAdminPassword();

            if (password.equals(adminPassword)) {
                return ResponseEntity.ok("Yönetici girişi başarılı."); // تم دخول المدير
            } else {
                return ResponseEntity.status(401).body("HATA: Yanlış yönetici şifresi!"); // باسورد المدير خطأ
            }
        }
        // ----------------------------------------------------------------

        // --- الكود الأصلي الخاص بك لخدمة العميل (Client CLI) ---
        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ResponseEntity.status(404).body("HATA: Kullanıcı bulunamadı!");
        }

        if (!user.getPassword().equals(password)) {
            return ResponseEntity.status(401).body("HATA: Yanlış şifre!");
        }

        return ResponseEntity.ok("Giriş başarılı.");
    }

    // 🟢 دالة جديدة للتحقق الفوري من توفر اسم المستخدم
    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = (userRepository.findByUsername(username) != null);
        return ResponseEntity.ok(exists);
    }
}