# Testing and verification

## التحقق الآلي الناجح

تم تشغيل التحقق على GitHub Actions لأن بيئة Arena المحلية لا تحتوي Java أو Android SDK.

- Run: `34796549165`
- Commit: `1a4cf3ec0f181bc9f182b3e09954fdf52fcfe496`
- JDK: Temurin `17.0.20.1`
- Gradle: `8.7`
- Android SDK: Platform `35` وBuild Tools `35.0.0`

الأوامر شُغّلت كخطوات منفصلة:

```bash
./gradlew testDebugUnitTest --stacktrace --console=plain
./gradlew lintDebug --stacktrace --console=plain
./gradlew assembleDebug --stacktrace --console=plain
```

النتيجة:

| الأمر | النتيجة |
|---|---|
| `testDebugUnitTest` | ناجح |
| `lintDebug` | ناجح |
| `assembleDebug` | ناجح |

عدد الاختبارات: **12 ناجحة، 0 فاشلة، 0 أخطاء، 0 متخطاة**.

## الاختبارات الموجودة

- تصنيف الامتدادات والـ MIME.
- فرز المجلدات والملفات في الاتجاهين.
- أولوية `cover` ثم `poster` ثم `folder` وFallback.
- إنشاء ZIP وفكّه.
- رفض مسارات Zip Slip.
- عتبات واتجاهات إيماءات الفيديو.
- اختبار إطلاق Activity موجود في `androidTest` لكنه لم يُشغّل لعدم وجود جهاز أو محاكي.

## فحوصات المصدر

تم تشغيل:

```bash
git diff --check
```

كما تم فحص:

- موارد strings العربية والإنجليزية وعدم وجود مراجع مفقودة.
- Version Catalog بصيغة TOML.
- حالات `Result` وعمليات الاستعادة.
- حماية مسارات النسخ وفك الأرشيف.
- إلغاء Jobs الخاصة بالبحث والتحليل والتصفح.
- تحرير موارد View on PC.

## ما لم يُتحقق منه

- تشغيل APK على جهاز Android حقيقي.
- تشغيل Emulator أو Instrumentation Tests.
- USB OTG وبطاقات SD الحقيقية.
- إشعار MediaSession وشاشة القفل.
- endpoint حقيقي لـ SMB/FTP/WebDAV أو Cloud OAuth.
- benchmark على مجلد يحوي آلاف الملفات.
- endpoint تنزيل حقيقي مع انقطاع واستئناف.

لذلك لا يُعد المشروع Production Ready من منظور اختبار الأجهزة والتكاملات الخارجية، حتى مع نجاح build وlint وunit tests.
