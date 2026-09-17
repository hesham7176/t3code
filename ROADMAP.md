# خطة التنفيذ المرحلية

## مكتمل ومتحقق آليًا

- [x] Android/Gradle/Compose/Material 3 وRTL واللغات.
- [x] طبقات UI وViewModel وData وDomain وMedia.
- [x] التصفح المحلي والفرز والبحث الأساسي.
- [x] Folder Cover بالترتيب cover ثم poster ثم folder وfallback/cache.
- [x] ZIP مع Zip Slip protection وconflict-safe extraction.
- [x] Media3 queue/resume/seek وgesture arbitration الأساسية.
- [x] عمليات التخزين المحلي مع conflict handling وrecycle metadata.
- [x] SAF browse/create/rename/search/copy/move عبر DocumentFile.
- [x] WorkManager download worker مع `.part` وRange وretry.
- [x] UTF-8 text repository مع حماية الحجم وحفظ ذري محلي.
- [x] 29 Unit Tests وLint وassembleDebug في CI.

## الخطوات المتبقية

- [ ] اختبار Instrumentation وsmoke flows على Emulator أو جهاز.
- [ ] اختبار USB OTG وSD الحقيقيين، بما في ذلك الصلاحيات والعمليات الكبيرة.
- [ ] إكمال حذف SAF الآمن عبر استراتيجية قابلة للاستعادة، لا حذفًا دائمًا صامتًا.
- [ ] ربط MediaEngine بMediaSession service والتحقق من notification وشاشة القفل.
- [ ] اختبار الفيديو والصوت والصور والدوران والسطوع والصوت على جهاز.
- [ ] اختبار مجلدات ضخمة وbenchmark للـthumbnail/search/analyzer.
- [ ] اختبار endpoint تنزيل حقيقي مع انقطاع واستئناف وتلف جزئي.
- [ ] تنفيذ SMB/FTP/WebDAV providers حقيقية مع authentication واختبارات integration.
- [ ] تنفيذ Cloud OAuth/provider حقيقي.
- [ ] تنفيذ Tabs/Windows فعليًا.
- [ ] إضافة UI tests وaccessibility tests للشاشات الرئيسية.

لا توجد إشارة إلى Production Ready قبل إكمال الاختبارات والتكاملات أعلاه. التصنيف الرسمي في `IMPLEMENTATION_STATUS.md`.
