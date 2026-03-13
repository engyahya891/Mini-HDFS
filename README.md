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

---
