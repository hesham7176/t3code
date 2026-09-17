# Testing and verification

## التحقق المؤكد

آخر تحقق نظيف للكود هو GitHub Actions run `35278719183` على commit التطبيق `e82ee10140351951befd545516bc4245983c96bb`.

تم تشغيل الأوامر كخطوات منفصلة:

```bash
./gradlew testDebugUnitTest --stacktrace --console=plain
./gradlew lintDebug --stacktrace --console=plain
./gradlew assembleDebug --stacktrace --console=plain
```

النتائج:

| الأمر | النتيجة |
|---|---|
| `testDebugUnitTest` | PASS |
| `lintDebug` | PASS |
| `assembleDebug` | PASS |

عدد الاختبارات: **29 ناجحة، 0 فاشلة، 0 أخطاء، 0 متخطاة**.

## الاختبارات ذات المعنى

- تصنيف امتدادات وأنواع الصور والفيديو والصوت والأرشيف.
- فرز الملفات والمجلدات بالاسم والحجم والاتجاه.
- أولوية `cover` ثم `poster` ثم `folder` وfallback وعدم قبول GIF كغلاف.
- ZIP round-trip وZip Slip وconflict-safe archive/extraction.
- سياسة أسماء الملفات ومنع escape وconflict handling.
- progress لعملية صفرية مكتملة.
- Gesture touch slop والتمييز بين horizontal/vertical والقطرية الغامضة.
- Storage Analyzer للأحجام recursive والفئات وprogress والجذر المفقود.
- UTF-8 strict read/write ورفض bytes غير صالحة وحد الحجم.
- Recycle metadata مع Unicode وseparator وlegacy fallback.
- HTTP/HTTPS وRange resume policy للتنزيلات.

## التغطية التي لم تُدّعَ

- لا توجد Unit Tests كاملة لـDocumentFile provider؛ تحتاج fake provider أو جهاز/اختبار instrumentation.
- لا توجد Unit Tests لـMedia3 الفعلي أو MediaSession notification.
- لا توجد UI tests للشاشات الرئيسية.
- `LaunchTest` موجود في `androidTest` لكنه لم يُشغّل.

## اختبار الجهاز

لم يتم تشغيل Android Emulator أو جهاز حقيقي. لذلك لم يتم التحقق فعليًا من:

- USB OTG وبطاقة SD.
- SAF providers الخاصة بالمصنعين.
- تشغيل الفيديو والصوت والإشعارات وشاشة القفل.
- الدوران والسطوع ومستوى الصوت والإيماءات على جهاز.
- View on PC من جهاز كمبيوتر.
- الأداء على آلاف الملفات.

## الشبكة والسحابة

SMB وFTP وWebDAV وCloud ما زالت عقودًا بلا providers حقيقية، ولذلك لا توجد integration tests أو authentication tests لها.

## البيئة المحلية

محليًا يفشل `./gradlew --version` وGradle قبل البدء لأن `java` غير موجود، لذلك تم تنفيذ التحقق الفعلي في GitHub Actions مع JDK 17 وAndroid SDK 35.
