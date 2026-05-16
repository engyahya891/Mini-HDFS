# 🌐 Mini HDFS — نظام ملفات موزع مصغر / Mini Dağıtık Dosya Sistemi

---

## 🇸🇦 الشرح باللغة العربية

### 🧭 لمحة عامة عن المشروع
مشروع Mini HDFS هو محاكاة تعليمية متقدمة لنظام الملفات الموزعة الشهير (Hadoop HDFS). لا تقتصر هذه المحاكاة على مجرد نقل الملفات، بل تطبق مفاهيم التجزئة (Chunking)، وتوزيع الأحمال، للبيانات. تم بناء المشروع باستخدام **Java Spring Boot** ويعتمد على بنية الخادم الرئيسي (Master) وخوادم التخزين الفرعية (Workers).

### 🏛️ المعمارية (Architecture)
يعتمد النظام على بنية (Master-Worker) ويتكون من ثلاثة أجزاء أساسية:
1. **الخادم الرئيسي (NameNode - Master):**
    - يعمل كمدير للنظام بأكمله، وهو نقطة الاتصال المركزية.
    - لا يخزن الملفات فعليًا، بل يحتفظ بـ "البيانات الوصفية" (Metadata) في قاعدة بيانات H2 الداخلية المدمجة عالية السرعة.
    - يدير الخوادم الفرعية من خلال تلقي نبضات الحياة (Heartbeats) والتقارير المستمرة حول مساحة التخزين الخاصة بهم.

2. **خوادم التخزين (DataNode - Worker):**
    - هي الخوادم المسؤولة عن العمل الشاق؛ أي تخزين البيانات فيزيائيًا على القرص الصلب محليًا.
    - يمكن تشغيل عدة خوادم تخزين في نفس الوقت (على منافذ/Ports مختلفة) لمحاكاة شبكة تخزين موزعة حقيقية.
    - تقدم تقارير دورية (Block Reports) وتنفذ أوامر القراءة والكتابة والحذف الواردة من العميل أو الخادم الرئيسي بدقة.

3. **العميل (Client):**
    - واجهة سطر أوامر تفاعلية (CLI) تتيح للمستخدم التعامل بسلاسة مع النظام.
    - توفر بيئة معزولة ونظيفة لكل مستخدم (عبر آليات تسجيل دخول بسيطة وتتبع مالك الملف Owner).

### 📂 بنية المشروع وشرح جميع الملفات (Project Structure & Files)
تم تقسيم المشروع باحترافية إلى وحدات مستقلة (Modules) لضمان سهولة الصيانة والتطوير، وتحتوي كل وحدة على ملفات مخصصة:

```text
Minihdfs/
├── 📁 namenode/                           # وحدة الخادم الرئيسي (Master)
│   ├── 📁 controller/                     # الموجهات (APIs)
│   │   ├── AdminController.java           # أوامر إدارة النظام والعمال
│   │   ├── AuthController.java            # تسجيل الدخول وإنشاء الحسابات
│   │   ├── ConfigController.java          # إعدادات النظام الديناميكية
│   │   ├── FileController.java            # رفع وتحميل وحذف الملفات
│   │   ├── LogController.java             # عرض وإدارة سجلات النظام
│   │   ├── MetricsController.java         # مقاييس أداء النظام والإحصائيات
│   │   ├── NotificationController.java    # إدارة إشعارات وتنبيهات النظام
│   │   └── WorkerController.java          # اتصالات العمال والنبضات (Heartbeat)
│   ├── 📁 model/                          # نماذج قواعد البيانات (Entities)
│   │   ├── BlockMetadata.java             # بيانات أجزاء الملفات (الكتل)
│   │   ├── FileMetadata.java              # البيانات الوصفية للملفات
│   │   ├── Notification.java              # نموذج التنبيهات والإشعارات
│   │   ├── SystemLog.java                 # نموذج سجلات الأحداث (Logs)
│   │   ├── User.java                      # بيانات المستخدمين
│   │   └── WorkerNode.java                # بيانات خوادم العمال
│   ├── 📁 repository/                     # مستودعات البيانات (JPA)
│   │   ├── BlockRepository.java           # عمليات قاعدة بيانات الكتل
│   │   ├── FileRepository.java            # عمليات قاعدة بيانات الملفات
│   │   ├── UserRepository.java            # عمليات قاعدة بيانات المستخدمين
│   │   └── WorkerRepository.java          # عمليات قاعدة بيانات العمال
│   ├── 📁 service/                        # الخدمات الخلفية
│   │   ├── LogService.java                # خدمة إدارة وحفظ السجلات
│   │   ├── NotificationService.java       # خدمة إرسال ومعالجة الإشعارات
│   │   ├── SystemConfigService.java       # خدمة التعامل مع إعدادات النظام
│   │   └── WorkerHealthService.java       # مراقبة صحة العمال واكتشاف الأعطال
│   ├── NameNodeApplication.java           # نقطة التشغيل الأساسية للمدير
│   └── Main.java                          # ملف إطلاق النظام الافتراضي
│
├── 📁 datanode/                           # وحدة خوادم التخزين (Worker)
│   ├── 📁 controller/                     # الموجهات (APIs)
│   │   ├── DataController.java            # استقبال الكتل، تحميلها، وحذفها
│   │   ├── ReplicationController.java     # النسخ الاحتياطي بأمر المدير
│   │   └── WorkerAdminController.java     # استقبال إشارة الإغلاق الفوري
│   ├── 📁 service/                        # الخدمات الخلفية
│   │   ├── BlockReportService.java        # إرسال تقرير بالملفات المحفوظة دورياً
│   │   └── HeartbeatService.java          # إرسال إشارة الحياة (Ping)
│   ├── DataNodeApplication.java           # التشغيل والتسجيل التلقائي
│   ├── MasterContext.java                 # حفظ سياق الاتصال بالمدير (IP)
│   └── Main.java                          # ملف الإطلاق الافتراضي
│
├── 📁 client/                             # وحدة العميل (CLI)
│   ├── ClientApplication.java             # واجهة سطر الأوامر التفاعلية (تقطيع الملفات)
│   ├── BlockAllocation.java               # خريطة توزيع الكتل المستلمة
│   └── Main.java                          # ملف الإطلاق الافتراضي
│
├── 📁 common/                             # وحدة النماذج المشتركة (Protocol)
│   ├── 📁 protocol/                       # نماذج الاتصال بين الوحدات (DTOs)
│   │   ├── BlockAllocation.java           # نموذج توزيع الكتل
│   │   ├── ClientUploadRequest.java       # طلب رفع ملف
│   │   ├── ClientUploadResponse.java      # استجابة الخادم لرفع الملف
│   │   ├── HeartbeatRequest.java          # طلب نبض الحياة
│   │   ├── StorageReportRequest.java      # تقرير مساحة التخزين
│   │   └── WorkerRegisterRequest.java     # طلب تسجيل عامل جديد
│   └── Main.java                          # ملف الإطلاق الافتراضي
│
└── 📁 data/                               # مجلد البيانات الفيزيائي
    ├── hdfs-metadata.mv.db                # قاعدة البيانات المدمجة (H2)
    ├── hdfs-metadata.trace.db             # ملف التتبع لقاعدة البيانات
    ├── worker_8081/                       # مجلد حفظ بيانات العامل الأول
    └── worker_8082/                       # مجلد حفظ بيانات العامل الثاني
```

### ✨ الميزات الرئيسية (Key Features)
- **تقسيم وملفات ضخمة (File Chunking):** يقوم العميل بتقطيع الملفات الكبيرة إلى كتل (Blocks) بحجم 64MB لرفعها بسرعة وتوزيعها على عدة خوادم تخزين، وعند التنزيل (Download) يتم تجميع الأجزاء بدقة بالغة وبشكل خفي عن المستخدم.
- **التكرار وتحمل الأخطاء (Replication Support):** يدعم الخادم الرئيسي حفظ البيانات المرفوعة على خوادم متعددة بحيث يستمر عمل النظام حتى إذا تعطل أحد خوادم التخزين بشكل مفاجئ.
- **نبضات الحياة (Heartbeat Mechanism):** نظام مدمج في كل خادم تخزين (DataNode) يُرسل إشارات دورية مجدولة للخادم الرئيسي، لتأكيد أنه لا يزال تعمل والتأكد من توافر الخادم.
- **عزل المستخدمين ومساحة خاصة (User Isolation):** تُمكّن واجهة سطر الأوامر المستخدم من اختيار اسم له عند الدخول، وبذلك لن يستطيع رؤية سوى الملفات المرفوعة بواسطته هو، مما يحقق عزلاً مناسباً للبيانات.
- **أدوات إدارة متكاملة (CLI):** يدعم نظام العميل أوامر متكاملة تشمل الرفع (`upload`)، التنزيل (`download`)، استعراض المجلد (`ls`)، والحذف الجذري (`delete`).

### ⚙️ التقنيات المستخدمة
- **Java 21 & Spring Boot 3.3.0**
- **Spring Data JPA & H2 Database** (لإدارة وحفظ ملفات الـ Metadata بمرونة)
- **Spring Web (REST APIs/RestTemplate)** للاتصالات المعقدة عبر الشبكة من دون تدخل يدوي بـمكونات (Sockets)
- **Java IO/NIO** لإدارة، وقراءة وتجميع أجزاء الملفات.
- **Maven** للاعتماديات وسهولة البناء.

### 🚀 كيفية التشغيل والاستخدام
1. **بناء المشروع:** نفذ الأمر `mvn clean package` من خلال التيرمينال.
2. **تشغيل الخادم الرئيسي (NameNode):** يعمل افتراضيًا على المنفذ `8080` ويمكن تشغيله من الـ IDE.
3. **تشغيل خوادم التخزين (DataNodes):** شغّلها على منافذ مختلفة (مثل `8081`، `8082`) وسيقوم كل سيرفر تلقائياً بإنشاء مجلد البيانات `data/worker_{port}/` الخاص به وإبلاغ الماستر بتواجده.
4. **تشغيل العميل (Client):** بمجرد التشغيل، أدخل عنوان سيرفر الماستر، واسم المستخدم، وابدأ بتجربة الأوامر الرائعة بكل سهولة!

---
<br>

## 🇹🇷 Türkçe Açıklama

### 🧭 Projeye Genel Bakış
Mini HDFS projesi, ünlü Hadoop Dağıtık Dosya Sistemi'nin (Hadoop HDFS) mükemmel ve eğitici bir simülasyonudur. Bu proje sadece normal bir dosya transferinden ibaret olmayıp; veri parçalama (Chunking), yük dağıtımı ve verinin sistem üzerinde tutarlılığını sağlama gibi kritik özelliklere sahiptir. Proje, modern **Java Spring Boot** altyapısı kullanılarak geliştirilmiş olup, Ana Sunucu (Master) ve Alt Depolama Sunucuları (Workers) mimarisini temel almaktadır.

### 🏛️ Mimari (Architecture)
Sistem (Master-Worker) mimarisi üzerine sağlam bir şekilde kuruludur ve üç ana bileşenden oluşur:
1. **Ana Sunucu (NameNode - Master):**
    - Tüm sistemin yöneticisi yani beyni olarak çalışır.
    - Dosyaları kesinlikle fiziksel olarak depolamaz. Aksine yüksek hızlı, gömülü H2 veritabanında dosyaların "Üst Verilerini" (Metadata) saklar.
    - Sürekli olarak alt sunuculardan gelen yaşam sinyallerini (Heartbeats) okur ve onların depolama kapasitelerini gözetler.

2. **Depolama Sunucuları (DataNode - Worker):**
    - Ağırlığı çeken sunuculardır; verileri diske fiziksel olarak lokal klasörlerde depolarlar.
    - Gerçek bir dağıtık depolama ağı oluşturmak için aynı anda birden fazla sunucu (farklı Port'larda) çalıştırılabilir.
    - İstemciden (Client) veya Ana sunucudan gelen okuma, yazma ve tamamen silme (delete) komutlarını mükemmel uygulayıp raporlar (Block Reports) sunarlar.

3. **İstemci (Client):**
    - Kullanıcının sistemle etkileşim kurmasını çok anlaşılır hale getiren interaktif bir komut satırı arayüzüdür (CLI).
    - Basit bir giriş mekanizması ile kullanıcılara özel alanlar ayarlar.

### 📂 Proje Yapısı ve Tüm Dosya Açıklamaları (Project Structure & Files)
Proje, bakım ve geliştirmeyi kolaylaştırmak adına profesyonelce bağımsız modüllere (Modules) ayrılmıştır ve her bir dosyanın özel bir işlevi vardır:

```text
Minihdfs/
├── 📁 namenode/                           # Ana Sunucu Modülü (Master)
│   ├── 📁 controller/                     # API Yönlendiricileri
│   │   ├── AdminController.java           # Sistem ve Worker yönetim komutları
│   │   ├── AuthController.java            # Kullanıcı girişi ve kayıt
│   │   ├── ConfigController.java          # Dinamik sistem yapılandırmaları
│   │   ├── FileController.java            # Dosya yükleme, indirme ve silme
│   │   ├── LogController.java             # Sistem loglarını görüntüleme/yönetme
│   │   ├── MetricsController.java         # Sistem performans metrikleri ve istatistikler
│   │   ├── NotificationController.java    # Sistem bildirimleri ve uyarıları yönetme
│   │   └── WorkerController.java          # Worker kayıtları ve Heartbeat sinyalleri
│   ├── 📁 model/                          # Veritabanı Modelleri (Entities)
│   │   ├── BlockMetadata.java             # Dosya blok parçalarının verileri
│   │   ├── FileMetadata.java              # Dosya üst verileri (Metadata)
│   │   ├── Notification.java              # Bildirimler ve uyarı modeli
│   │   ├── SystemLog.java                 # Sistem olay günlükleri modeli (Logs)
│   │   ├── User.java                      # Kullanıcı bilgileri
│   │   └── WorkerNode.java                # İşçi sunucu bilgileri
│   ├── 📁 repository/                     # Veritabanı İşlemleri (JPA)
│   │   ├── BlockRepository.java           # Blok veritabanı CRUD işlemleri
│   │   ├── FileRepository.java            # Dosya veritabanı CRUD işlemleri
│   │   ├── UserRepository.java            # Kullanıcı veritabanı CRUD işlemleri
│   │   └── WorkerRepository.java          # Worker veritabanı CRUD işlemleri
│   ├── 📁 service/                        # Arka Plan Servisleri
│   │   ├── LogService.java                # Sistem günlüklerini (log) yönetme servisi
│   │   ├── NotificationService.java       # Bildirim gönderme ve işleme servisi
│   │   ├── SystemConfigService.java       # Sistem ayarlarını yönetme servisi
│   │   └── WorkerHealthService.java       # Worker sağlığını izleme ve arıza tespiti
│   ├── NameNodeApplication.java           # Master ana çalıştırma sınıfı
│   └── Main.java                          # Varsayılan başlangıç dosyası
│
├── 📁 datanode/                           # Depolama Sunucusu Modülü (Worker)
│   ├── 📁 controller/                     # API Yönlendiricileri
│   │   ├── DataController.java            # Blokları kaydetme, indirme ve fiziksel silme
│   │   ├── ReplicationController.java     # Master'ın emriyle blok yedekleme (Replication)
│   │   └── WorkerAdminController.java     # Acil kapatma emrini (Shutdown) karşılama
│   ├── 📁 service/                        # Arka Plan Servisleri
│   │   ├── BlockReportService.java        # Diskteki blokları düzenli raporlama
│   │   └── HeartbeatService.java          # Sürekli "Hayattayım" sinyali gönderme
│   ├── DataNodeApplication.java           # Worker uygulamasını başlatma ve kaydolma
│   ├── MasterContext.java                 # Master bağlantı bağlamını (IP) saklama
│   └── Main.java                          # Varsayılan başlangıç dosyası
│
├── 📁 client/                             # İstemci Modülü (CLI)
│   ├── ClientApplication.java             # Etkileşimli CLI ve dosya parçalama (Chunking)
│   ├── BlockAllocation.java               # Alınan blok dağılım haritası
│   └── Main.java                          # İstemciyi başlatma
│
├── 📁 common/                             # Ortak Veri Transfer Modelleri (Protocol)
│   ├── 📁 protocol/                       # Modüller Arası Veri Transfer Nesneleri (DTO)
│   │   ├── BlockAllocation.java           # Blok dağılım modeli
│   │   ├── ClientUploadRequest.java       # İstemci dosya yükleme isteği
│   │   ├── ClientUploadResponse.java      # Yükleme işlemine verilen sunucu yanıtı
│   │   ├── HeartbeatRequest.java          # Yaşam sinyali isteği
│   │   ├── StorageReportRequest.java      # Depolama alanı durumu raporu
│   │   └── WorkerRegisterRequest.java     # Yeni işçi kayıt isteği
│   └── Main.java                          # Varsayılan başlangıç dosyası
│
└── 📁 data/                               # Fiziksel Veri ve Veritabanı
    ├── hdfs-metadata.mv.db                # Gömülü (H2) sistem veritabanı dosyası
    ├── hdfs-metadata.trace.db             # Veritabanı izleme (trace) dosyası
    ├── worker_8081/                       # Birinci Worker'ın blok saklama klasörü
    └── worker_8082/                       # İkinci Worker'ın blok saklama klasörü
```

### ✨ Temel Özellikler (Key Features)
- **Gelişmiş Dosya Parçalama (File Chunking):** İstemci, büyük çaplı dosyaları sistemi yormamak için yükleme esnasında yaklaşık 64 MB'lık parçalara (Blocks) böler. Ardından indirme talebinde, bu parçaları hatasız bir şekilde geri birleştirir.
- **Yedekleme ve Hata Toleransı (Replication Support):** Sistemin sürdürülebilirliği için yüklenen veriler Ana sunucu tarafından birden fazla depolama sunucusunda kopyalanabilir şekilde tasarlanmıştır. Bu da bir DataNode aniden kapansa dahi verilerin güvenle kalmasını sağlar.
- **Yaşam Sinyali Mekanizması (Heartbeat Mechanism):** Her DataNode'a özel eklenmiş periyodik (Scheduled) sistem parçası sayesinde, Ana Sunucu hangi aktörlerin anlık olarak çalıştığını ve disk boyutlarını sürekli haberdar edilir.
- **Kullanıcı Güvenliği/İzolasyonu (User Isolation):** İstemci komut satırına adınızla girdiğinizde, dosyaların sahibi (Owner) statüsüne geçersiniz. Sistem size diğer kişilerin yüklediği dosyaları kesinlikle göstermez.
- **Kapsamlı Komut Araçları (CLI):** Projenin arayüzü; dosya yükleme (`upload`), indirme (`download`), klasör içeriği görüntüleme (`ls`) ve kalıcı silme (`delete`) operasyonlarını çok zengin ve net geribildirimlerle yerine getirir.

### ⚙️ Kullanılan Teknolojiler
- **Java 21 & Spring Boot 3.3.0**
- **Spring Data JPA & H2 Veritabanı** (Metadata'ları esnek okuyup yazmak için)
- **Spring Web (REST API / RestTemplate)** Düşük seviyeli karmaşık socket kodlamasına girmeden, stabil sunucu haberleşmesini sağlama
- **Java IO/NIO** Cihazdaki dosyaları performanslı işlemek için
- **Maven** Tüm kütüphaneleri ve projeyi derlemek için.

### 🚀 Çalıştırma ve Kullanım Talimatları
1. **Projeyi Derleme:** Klasör dizininde terminalden `mvn clean package` komutunu çalıştırın.
2. **Ana Sunucunun Başlatılması (NameNode):** Varsayılan olarak `8080` portunda çalışır, direkt IDE'nizden ayağa kaldırabilirsiniz.
3. **Depolama Sunucularının Başlatılması (DataNodes):** Farklı port konfigürasyonlarıyla (örn. `8081`, `8082`) aynı cihazda birden fazla çalıştırabilirsiniz. Çalıştığında kendi klasörü `data/worker_{port}/`'ı yaratıp Ana Sunucuya haber yollar.
4. **İstemcinin Çalıştırılması (Client):** Arayüz başladığında sizden Ana Sunucunun IP numarasını ve bir takma ad isteyecek. Bunları girip harika komutlarla sistemi test etmeye başlayabilirsiniz!
