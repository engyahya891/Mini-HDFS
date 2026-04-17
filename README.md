<div align="center">

# 🌐 Mini HDFS Core System
### **محرك نظام الملفات الموزعة — الخادم الأساسي (Back-end Code)**

![Version](https://img.shields.io/badge/الإصدار-1.0.0-indigo?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-6DB33F?style=for-the-badge&logo=springboot)
![H2 Database](https://img.shields.io/badge/H2_Database-Embedded-0096D6?style=for-the-badge&logo=h2)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven)

**نواة خادم متقدمة ومستقلة تحاكي معمارية (Hadoop HDFS)، مجهزة بأحدث تقنيات Java لضمان إدارة الملفات الضخمة وتوزيعها عبر الشبكات بأمان وبأعلى موثوقية.**

</div>

---

## 🇸🇦 الشرح باللغة العربية (Arabic)

### 📌 نظرة عامة
هذا الخادم (Back-end) هو العقل المدبر لنظام **Mini HDFS**. يهدف النظام إلى استيعاب ملفات ضخمة جداً لا يمكن لجهاز كمبيوتر واحد تحملها، عن طريق تقسيمها رياضياً وتوزيعها على أجهزة سيرفرات متعددة. يتبنى النظام معمارية (Master-Worker) متينة لضمان التوافر العالي للبيانات (High Availability)، وتحمل خطأ انقطاع الخوادم (Fault Tolerance).

> **مناسب لـ:** بناء أنظمة التخزين السحابية، مشاريع دراسة الأنظمة الموزعة (Distributed Systems)، وتعلم كيف تعمل تقنيات Big Data من الداخل.

---

### ✨ المعمارية التقنية وآلية العمل

ينقسم النظام برمجياً إلى ثلاث وحدات رئيسية (Modules) تعمل بتزامن هندسي كامل:

#### 1. الخادم الرئيسي (NameNode - Master)
- **الإدارة المركزية الذكية:** لا يحتفظ بالملفات الفعلية ولن يستهلك مساحة للبيانات، بل يعمل كـ "دماغ النظام الخفيف".
- **قاعدة البيانات الوصفية (Metadata):** يستخدم قاعدة **H2 Database** مدمجة وعالية السرعة لتخزين المخططات، مثل: (الملف X يمتلك 4 أجزاء، ويوجد كل جزء في الخادم A و B).
- **مراقبة النبض (Heartbeats):** يستقبل إشارات نبضية (Heartbeat) كل بضع ثوانٍ من كل عقدة (DataNode) متصلة ليتأكد من أنها لم تنطفئ، وإذا انطفأت، يقوم بالاستعانة بالنسخ الاحتياطية.

#### 2. خوادم التخزين الفرعية (DataNode - Worker)
- **العضلات المنفذة:** هذه هي السيرفرات التي يتم فيها كتابة وقراءة وعمليات حذف أجزاء الملفات (Chunks) بفيزيائية تامة على الأقراص الصلبة.
- **تقارير الكتل (Block Reports):** تقوم العقدة بإرسال تقارير مجدولة للماستر تخبره بمساحة قرصها المتبقية والكتل التي بحوزتها حالياً.
- **المرونة (Scalability):** صمم النظام لتتمكن من تشغيل (1 أو 10 أو 100) خادم فرعي في نفس الوقت (عبر تغيير الـ Port) لتوسيع سعة التخزين فورا.

#### 3. العميل الخفي (CLI Client)
- هو الواجهة الخدمية الوسيطة بين المستخدم والخادم. يقوم بتقطيع أي ملف صخم (مثلاً بحجم 1GB) إلى كتل صغيرة (بمعدل ثابت 64MB للكتلة)، ومن ثم يقوم برفعها بشكل موازي وسريع إلى الخوادم الفرعية، وعند طلب التحميل يقوم العميل بجمعها بخفاء ودمجها ليخرج لك الملف الأصلي سالماً.

---

### 🔗 آلية الربط المتطورة مع واجهة المستخدم (Front-end Integration)

تم تصميم هذا الخادم الموزع ليكون **منفتحاً** تماماً ويمكن التحكم به من لوحة تحكم رسومية خارجية (Admin Panel) وذلك عبر توفيره **(RESTful APIs)** قوية ومتكاملة:

- **تصدير المخطط الشبكي (Exporting Topology):** تقوم واجهات الـ NameNode بإتاحة Endpoint خاص يسرد تفاصيل العقد (DataNodes) المتصلة حديثاً وقوة نبضها. يقوم الـ (Front-end) بسحب هذه البيانات ورسم **خريطة شبكية تفاعلية** تبين الخوادم الخضراء (النشطة) والحمراء (الملغاة).
- **إدارة الملفات الافتراضية (Virtual File Manager):** الواجهة لا تتصل بقاعدة البيانات إطلاقاً! بل ترسل طلبات HTTP للـ NameNode للاستفسار عن المجلدات. يقوم الخادم بتجميع البيانات الوصفية من الـ H2 وإرسالها كـ JSON، لتقوم الواجهة بعرضها على شكل صناديق وملفات أنيقة للمستخدم.
- **تحديث الإحصائيات (Metrics Aggregation):** يقوم الماستر بحساب استهلاك ومساحات الأقراص لكل الـ Workers وعمليات الإدخال والإخراج، ويرسل هذه الأرقام للواجهة (Front-end) التي تقوم بتحليلها ورسم **مخططات بيانية (Charts)** جميلة.

---

### 📁 هيكل المشروع (Project Structure)

```text
Back-end/
├── common/                  # كلاسات مشتركة (Models, Utilities) تستخدم عبر كافة الوحدات
├── namenode/                # وحدة الخادم الرئيسي (Master) للبيانات الوصفية
│   └── src/main/java...
│       ├── controller/      # واجهات الـ REST API للتواصل مع الواجهة الأمامية والعميل
│       ├── service/         # منطق العمليات وإدارة نبضات الحياة (Heartbeats) والمخطط
│       ├── repository/      # طبقة الاتصال بقاعدة البيانات (H2)
│       └── model/           # كائنات البيانات الوصفية (Metadata)
├── datanode/                # وحدة خوادم التخزين الفرعية (Workers)
│   └── src/main/java...
│       ├── controller/      # استقبال أوامر الرفع والحذف من الـ NameNode
│       └── service/         # عمليات التعامل مع القرص الصلب الفعلي وإرسال التقارير
├── client/                  # وحدة العميل (CLI) التي ينفذ منها المستخدم الأوامر
│   └── ClientApplication.java # نقطة الدخول، يتولى تقطيع الملفات وإرسالها متوازياً
└── pom.xml                  # ملف إدارة الاعتماديات (Maven) وبناء الوحدات
```

---
<br>

<div align="center">

*   *   *

</div>

<br>

## 🇹🇷 Türkçe Açıklama (Turkish)

### 📌 Projenin Genel Bakışı
Bu Sunucu (Back-end), **Mini HDFS** sisteminin işletim beynidir. Projenin tek bir bilgisayarın taşıyamayacağı büyüklükteki dosyaları matematiksel parçalara ayırıp çoklu cihazlara dağıtarak depolaması amaçlanmıştır. Sistem, Yüksek Erişilebilirlik (High Availability) ve sunucu arızalarına karşı tolerans (Fault Tolerance) sağlamak adına sarsılmaz bir Yönetici-İşçi (Master-Worker) mimarisini benimser.

> **Kullanım Alanı:** Bulut depolama sistemleri inşası, dağıtık sistemler (Distributed Systems) üzerine araştırmalar ve Big Data (Büyük Veri) teknolojilerinin temel çalışma prensiplerini kavrama.

---

### ✨ Teknik Mimari ve Çalışma Prensibi

Sistemin kod altyapısı, mühendislik harikası bir senkronizasyonla çalışan üç ana modüle (Module) bölünmüştür:

#### 1. Ana Sunucu (NameNode - Master)
- **Akıllı Merkezi Yönetim:** Fiziksel bir dosya saklamaz ve veri alanı tüketmez. Bunun yerine "Ağırlıksız Sistem Beyni" rolünü üstlenir.
- **Üst Veri Tabanı (Metadata):** Şemaları saklamak için doğrudan gömülü, süper hızlı **H2 Database** kullanır. (Örn: X dosyası 4 parça, parçalar A ve B düğümünde).
- **Kalp Atışı İzleme (Heartbeats):** Sisteme bağlı her bir DataNode'dan (Depolama Düğümü) birkaç saniyede bir kalp atışı (Heartbeat) sinyali alır. Eğer bir düğümden sinyal kesilirse, hemen diğer yedeği devreye sokar.

#### 2. Depolama Sunucuları (DataNode - Worker)
- **İcracı Kaslar:** Tüm dosya parçalarının (Chunks) fiziksel olarak sabit diske yazıldığı, okunduğu ve silindiği güçlü sunuculardır.
- **Blok Raporları (Block Reports):** Aktif Düğüm (Node), ne kadar disk alanının kaldığını ve üzerinde bulunan veri bloklarını periyodik olarak Ana Sunucuya raporlar.
- **Esnek Ölçeklendirme (Scalability):** Sistem, depolama alanını saniyeler içinde arttırabilmek amacıyla, Port numaralarını değiştirerek (1, 10 veya 100) adet alt sunucuyu aynı anda çalıştırabilmenize imkan tanır.

#### 3. Konsol İstemcisi (CLI Client)
- Kullanıcı ve sunucu arasındaki görünmez aracıdır (CLI). Yükleyeceğiniz devasa bir dosyayı (Örn: 1GB) alır, minik bloklara (standart 64MB/Blok) böler ve bu parçaları asenkron saniyeler içinde alt sunuculara dağıtır. Geri indirmek istediğinizde de bu parçaları tamamen gizli bir şekilde mükemmelce geri toplayarak asıl dosyayı sizlere teslim eder.

---

### 🔗 Ön Yüz İle Gelişmiş Entegrasyon (Front-end Integration)

Bu arka uç projesi tamamen **açık** standartlarla tasarlanmış ve dış bir grafiksel panel (Admin Panel) aracılığıyla profesyonel **(RESTful APIs)** ile rahatlıkla entegre olabilecek kapasitededir:

- **Ağ Topolojisini Paylaşmak (Exporting Topology):** NameNode, anlık bağlanan düğümleri (DataNodes) ve onların bağlantı güçlerini raporlayan özel bir Endpoint açar. Ön Yüz (Front-end) bu bilgiyi sürekli Fetch ederek panoda aktif (yeşil) ve erişilemeyen (kırmızı) cihazları gösteren **İnteraktif bir Ağ Haritası** çizer.
- **Sanal Dosya Yöneticisi (Virtual File Manager):** Arayüz asla doğrudan veritabanına bağlanmaz! İnternet üzerinden NameNode'a HTTP istekleri yollar, Sunucu H2 veritabanından klasör üstverisini okur ve formatlanmış JSON dosyasını panele cevap döner; Front-end bu paketi estetik ikonlara sahip akıllı bir "Dosya Yöneticisine" dönüştürür.
- **Dinamik İstatistikler (Metrics Aggregation):** Master sunucu; çalışan tüm işçilerin tüketimlerini, disk doluluk oranlarını ve işlem sayılarını toplar, derler. Elde edilen değerleri doğrudan panele iletir, ve bu veriler arayüz tarafında hayranlık uyandıran **grafiklerle (Charts)** görselleştirilir.

---

### 📁 Proje Klasör Yapısı (Project Structure)

```text
Back-end/
├── common/                  # Tüm modüllerde ortak kullanılan yardımcı sınıflar ve Modeller
├── namenode/                # Üst veri (Metadata) yönetimini sağlayan Ana Sunucu (Master) modülü
│   └── src/main/java...
│       ├── controller/      # Ön yüz ve İstemci ile iletişimi kuran REST API uçları
│       ├── service/         # İş mantığı, Kalp atışı (Heartbeat) takibi ve cihaz yönetimi
│       ├── repository/      # (H2) Veritabanı ile doğrudan iletişim kuran erişim katmanı
│       └── model/           # Veri modelleri ve JSON referansları
├── datanode/                # Verilerin bloklar halinde fiziksel saklandığı (Worker) modülü
│   └── src/main/java...
│       ├── controller/      # NameNode'dan gelen dosyayı yazma ve silme emirlerini algılar
│       └── service/         # Sabit diskteki işlemleri yönetir ve kapasite raporları atar
├── client/                  # Komut satırı işlemlerinin yapıldığı İstemci modülü
│   └── ClientApplication.java # Kullanıcı emirlerini algılayıp dev dosyaları parçalar ve gönderir
└── pom.xml                  # Maven proje bağımlılıkları ve modül yönetim ayarları
```

