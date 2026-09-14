# خطة التنفيذ المرحلية

## المنجز والمتحقق آليًا

- [x] مشروع Android وGradle Wrapper وCompose وMaterial 3.
- [x] Manifest وFileProvider وموارد العربية والإنجليزية.
- [x] طبقات `data/domain/media/ui`.
- [x] الشاشة الرئيسية ومدير الملفات والفرز وطرق العرض.
- [x] Folder Cover وthumbnail cache.
- [x] عمليات التخزين المحلي وسلة المحذوفات وخصائص الملفات.
- [x] Media3 للفيديو والصوت والصور، queue، resume، gesture policy.
- [x] ZIP مع حماية Zip Slip.
- [x] Storage Analyzer والبحث ومحرر النصوص ومدير التطبيقات.
- [x] WorkManager Download Worker وخادم View on PC.
- [x] RTL والعربية والإنجليزية وDark Mode.
- [x] 12 Unit Tests وLint وassembleDebug في CI.

## ما يحتاج تحققًا خارجيًا

- [ ] تشغيل Instrumentation وsmoke flows على Emulator.
- [ ] USB OTG وSD حقيقيان: mount، SAF، صلاحيات، قراءة وعمليات كبيرة.
- [ ] تشغيل فيديو/صوت فعلي وإشعار MediaSession وشاشة القفل.
- [ ] اختبار دوران الشاشة وملء الشاشة والسطوع والصوت على جهاز.
- [ ] benchmark لمجلدات ضخمة وصور/فيديوهات كثيرة.
- [ ] endpoint تنزيل حقيقي مع timeout وإعادة المحاولة.
- [ ] مزود SMB/FTP/WebDAV فعلي مع authentication.
- [ ] Cloud OAuth/provider فعلي.

الحالة التفصيلية والتصنيف الرسمي في `IMPLEMENTATION_STATUS.md`.
