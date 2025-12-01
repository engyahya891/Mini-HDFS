# 🌐 Mini HDFS — نظام ملفات موزع مصغر

> 🎓 مشروع تعليمي لمحاكاة مفهوم Hadoop HDFS (نموذج مبسّط)  
> الهدف: فهم عملي لمفاهيم النظم الموزعة، برمجة الشبكات، والتزامن.

---

## 🧭 لمحة عامة

Mini HDFS هو نموذج تعليمي مبسّط لنظام ملفات موزع. يقوم بتقسيم الملفات إلى كتل (chunks) وتوزيعها على خوادم تخزين (DataNodes) مع خادم تحكّم مركزي (NameNode) يدير البيانات الوصفية (metadata) وحالة العقد.

هذا المشروع مخصص للأغراض التعليمية فقط — لتجربة أفكار مثل التجزئة، التكرار (replication)، heartbeat، والتعامل عبر الـ sockets.

---

## ⚙️ المعمارية (Master–Slave)

- NameNode (الخادم الرئيسي)
  - يحتفظ بالـ metadata: أسماء الملفات، حجمها، ومواقع الكتل.
  - يستقبل heartbeats من الـ DataNodes ويقرر إعادة التكرار عند الضرورة.

- DataNode (خادم التخزين)
  - يخزن كتل البيانات فعليًا على القرص.
  - ينفذ أوامر القراءة والكتابة ويُبَلِّغ حالة التشغيل بصورة دورية.

- Client (واجهة سطر الأوامر)
  - يتيح تنفيذ أوامر مثل put/get/ls/mkdir للتفاعل مع النظام.

---

## ✨ الميزات الرئيسية

- تقسيم الملفات إلى كتل وتوزيعها عبر DataNodes.
- إمكانية إعادة التكرار (Replication) لزيادة تحمل الأخطاء.
- واجهة سطر أوامر بسيطة تشبه أوامر نظام Linux.
- تصميم يسمح بإضافة المزيد من DataNodes بسهولة (قابلية التوسع).

---

## 🧩 التقنيات والأدوات المستخدمة

| التقنية | الاستخدام |
|--------:|:----------|
| Java (8+) | لغة التطوير الأساسية |
| Java IO / NIO | إدارة الملفات وتدفقات البيانات |
| Java Sockets (TCP) | الاتصال بين العقد (NameNode/DataNode/Client) |
| Multithreading | معالجة متزامنة لطلبات متعددة |
| Maven | إدارة البناء والاعتماديات |

---

## 📂 هيكل المشروع (اقتراحي للعرض)

```
Mini-HDFS/
│
├── src/
│   ├── main/java/
│   │   ├── namenode/
│   │   │   └── NameNode.java
│   │   ├── datanode/
│   │   │   └── DataNode.java
│   │   └── client/
│   │       └── Client.java
│   │
│   └── main/resources/
│       └── config.properties
│
├── pom.xml
└── README.md
```

ملخص الملفات:
- NameNode.java — إدارة النظام والـ metadata.
- DataNode.java — تخزين الكتل والتعامل مع الأوامر.
- Client.java — CLI للتفاعل مع النظام.
- config.properties — إعدادات الاتصال والمسارات.

---

## 🧭 كيفية التشغيل (مطلوب)

المتطلبات:
- Java 8 أو أحدث
- Maven

تشغيل محلي (مثال):

1. بناء المشروع:
```bash
mvn clean package
```

2. تشغيل NameNode (على المضيف والمنفذ الافتراضي المحدد في config.properties):
```bash
java -jar target/mini-hdfs.jar --namenode
```

3. تشغيل واحد أو أكثر من DataNodes (كل DataNode على منفذ ومسار تخزين مختلف):
```bash
java -jar target/mini-hdfs.jar --datanode --storage-dir=/tmp/dn1 --port=5001
java -jar target/mini-hdfs.jar --datanode --storage-dir=/tmp/dn2 --port=5002
```

4. تشغيل العميل (Client) للتفاعل عبر سطر الأوامر:
```bash
java -jar target/mini-hdfs.jar --client
```

ملاحظة: إذا لم يكن مشروعك مجمّعًا في jar موحد، يمكنك تشغيل كل فئة من IDE أو عبر الأوامر بتحديد الـ classpath.

---

## 📘 أوامر Client (أمثلة)

- رفع ملف إلى النظام:
```bash
put localfile.txt /remote/path/file.txt
```

- تنزيل ملف من النظام:
```bash
get /remote/path/file.txt localfile.txt
```

- عرض محتوى الدليل:
```bash
ls /remote/path
```

- إنشاء مجلد:
```bash
mkdir /remote/path/newdir
```

---

## 🔧 إعدادات مقترحة (config.properties مثال)

```properties
# NameNode
namenode.host=localhost
namenode.port=9000

# افتراضي تكرار الكتل
replication.factor=3

# Timeouts/heartbeat
datanode.heartbeat.interval=5000
```

---

## ✅ نصائح للاختبار محليًا

- شغّل NameNode في نافذة طرفية منفصلة.
- شغّل عدة DataNodes مع مسارات تخزين مختلفة (مجلدات مؤقتة).
- استخدم Client لوضع ملفات كبيرة وملاحظة تقسيمها واسترجاعها.
- جرّب إيقاف أحد DataNodes ولاحظ إعادة التكرار إذا تم تنفيذها.

---

## 🔮 أفكار للتطوير المستقبلي

- لوحة ويب لمراقبة حالة العقد والـ metadata.
- تحسين آلية الـ replication وطلبات إعادة التوازن (rebalancing).
- إضافة آلية Authentication وAuthorization.
- تحسين الـ logging واستخدام مكتبة Logging مثل Logback/SLF4J.
- إضافة اختبارات وحدة (unit tests) وتكامل (integration tests).
- دعم تخزين متماثل قابِل للتهيئة ديناميكياً.

---

## 📚 الترخيص

المشروع للأغراض التعليمية فقط. لا يُسمح بالاستخدام التجاري من دون إذن صاحب المشروع.

---

## 👨‍💻 المطور

يحيى حمشو — Yahya Hamsho  
البريد الإلكتروني: your.email@example.com  
GitHub: https://github.com/YahyaHamsho

```
