package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.User;
import com.hdfs.namenode.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // 🟢 1. تسجيل مستخدم جديد (Register)
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username, @RequestParam String password) {
        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.status(409).body("HATA: Bu kullanıcı adı zaten mevcut!"); // خطأ: المستخدم موجود
        }

        User newUser = new User(username, password);
        userRepository.save(newUser);
        return ResponseEntity.ok("Kayıt başarılı! Artık giriş yapabilirsiniz."); // تم التسجيل بنجاح
    }

    // 🟢 2. تسجيل الدخول (Login)
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ResponseEntity.status(404).body("HATA: Kullanıcı bulunamadı!"); // خطأ: المستخدم غير موجود
        }

        if (!user.getPassword().equals(password)) {
            return ResponseEntity.status(401).body("HATA: Yanlış şifre!"); // خطأ: كلمة المرور خاطئة
        }

        return ResponseEntity.ok("Giriş başarılı."); // تم الدخول بنجاح
    }
    // 🟢 دالة جديدة للتحقق الفوري من توفر اسم المستخدم
    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = (userRepository.findByUsername(username) != null);
        return ResponseEntity.ok(exists);
    }
}