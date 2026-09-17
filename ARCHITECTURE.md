# Architecture

## الطبقات

- `data`: الملفات وSAF والتفضيلات والتنزيلات والتحليل والتطبيقات.
- `domain`: نماذج مستقلة، الفرز، أنواع الملفات، Folder Covers، ZIP، سياسات الأمان والإيماءات.
- `media`: Media3/ExoPlayer وMediaSession وmetadata وthumbnails وحفظ موضع التشغيل.
- `ui`: Compose screens وViewModel وStateFlow والتنقل والحالة.

## التصفح والـSAF

`ExplorerViewModel` يختار بين `FileRepository` للتخزين المحلي و`DocumentFile` داخل `FileRepository` و`SafStorageRepository` لمواقع SAF. URI لا يمر إلى `java.io.File`. توجد صلاحيات persisted tree، وتدعم SAF القراءة والفرز والبحث وإنشاء الملف/المجلد وإعادة التسمية والنسخ والنقل عبر streams.

الحذف من SAF غير مفعّل لأنه لا يمر عبر سلة المحذوفات. تعرض العمليات غير المدعومة حالة خطأ واضحة بدل نجاح وهمي.

## العمليات المحلية

`FileOperationManager` ينفذ النسخ والنقل recursive على `Dispatchers.IO`، مع conflict-safe names، canonical containment checks، progress، cancellation checks، وسلة محذوفات تحفظ المسار الأصلي بترميز آمن. يتم إعادة رمي `CancellationException` بدل تحويل الإلغاء إلى فشل عادي مضلل.

## الوسائط

`MediaEngine` يغلف ExoPlayer، queue، resume position، seek، speed، state، وأخطاء التشغيل. `MediaScreen` يوفر فيديو وصوتًا وصورًا وzoom/pan وgallery وfullscreen واتجاهًا وإيماءات مقفلة الاتجاه بعد touch slop.

`ExplorerPlaybackService` و`MediaSession` موجودان، لكن ربط محرك الواجهة بالخدمة والإشعار والتحقق من شاشة القفل يحتاجان اختبار جهاز فعلي؛ لذلك الحالة PARTIAL وليست DONE.

## Folder Covers وZIP

`FolderCoverResolver` يحافظ على الأولوية `cover` ثم `poster` ثم `folder`، ويتحقق من jpg/jpeg/png/webp مع fallback وcache. `ArchiveManager` ينشئ ويفك ZIP عبر ملف مؤقت، ويدعم conflict-safe extraction ويحمي canonical destination من Zip Slip.

## التحليل والتنزيل والنصوص

`StorageAnalyzer` يحسب الأحجام recursive حسب الفئة مع progress وإلغاء. `DownloadWorker` يستخدم WorkManager وملف `.part` وHTTP Range وretry وatomic finalization ويمنع FTP/البروتوكولات غير HTTP(S). محرر النصوص يفرض UTF-8 strict، حد 5 MB، حفظًا ذريًا محليًا ومسارات SAF عبر ContentResolver.

## View on PC والشبكات

`ViewOnPcServer` خادم قراءة فقط محمي بتوكن، مع canonical path checks وHTML escaping و401/403/404 وshutdown. لا يوجد upload. `RemoteFileSystem` و`CloudProvider` عقود فقط؛ SMB وFTP وWebDAV وCloud OAuth ليست providers فعلية.

## التحقق

التفاصيل في `IMPLEMENTATION_STATUS.md` و`TESTING.md`. آخر تحقق نظيف مؤكد هو run `35278719183`، وAPK metadata في run `35279113727`.
