# 🌐 Mini HDFS (Mini Hadoop Distributed File System)

> 🎓 **A simplified educational simulation of Hadoop HDFS**  
> Developed for understanding **Distributed Systems**, **Network Programming**, and **Concurrency** in practice.

---

## 🇸🇦 الوصف الاحترافي للمشروع (Project Description in Arabic)

### 🔹 نبذة عامة
يُعد مشروع **Mini HDFS** نموذجًا مبسطًا يحاكي آلية عمل نظام الملفات الموزع الشهير **Hadoop Distributed File System (HDFS)**.  
يهدف المشروع إلى بناء نظام تخزين موزع قادر على إدارة الملفات الضخمة من خلال **تجزئتها إلى كتل (Chunks)** وتوزيعها عبر **عدة خوادم (DataNodes)**، بإشراف **خادم رئيسي (NameNode)** يتولى مهام التحكم والإدارة.

تم تطوير هذا المشروع لأغراض تعليمية بحتة، تهدف إلى تعزيز الفهم العملي لمفاهيم **النظم الموزعة (Distributed Systems)**، و**برمجة الشبكات (Socket Programming)**، بالإضافة إلى **التعامل مع التزامن (Concurrency)** في الأنظمة المتعددة العملاء.

---

### ⚙️ هيكلية النظام (System Architecture)

يعتمد النظام على بنية **Master–Slave** الكلاسيكية، ويتكون من ثلاث مكونات رئيسية:

#### 🧠 1. NameNode (الخادم الرئيسي)
- يمثل نقطة التحكم المركزية في النظام.
- يحتفظ بـ **البيانات الوصفية (Metadata)** مثل أسماء الملفات، أحجامها، ومواقع الكتل.
- يتابع حالة خوادم التخزين باستخدام آلية **Heartbeat** لضمان الجاهزية.

#### 💾 2. DataNode (خادم التخزين)
- مسؤول عن **التخزين الفعلي للبيانات**.
- ينفذ أوامر القراءة والكتابة من الـ NameNode أو الـ Client.
- يرسل **Heartbeats** بشكل دوري لتأكيد الاتصال واستمرارية العمل.

#### 💻 3. Client (تطبيق سطر الأوامر)
- يوفّر واجهة تفاعلية للتعامل مع النظام.
- يدعم أوامر شبيهة بـ Linux مثل:
  ```bash
  put <file>     # رفع ملف إلى النظام
  get <file>     # تنزيل ملف من النظام
  ls             # عرض الملفات والمجلدات
  mkdir <dir>    # إنشاء مجلد جديد
