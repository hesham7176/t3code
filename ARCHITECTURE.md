# Architecture

## الطبقات

- `data`: الوصول إلى الملفات والتخزين والتفضيلات والتحميلات.
- `domain`: نماذج مستقلة، فرز، أغلفة المجلدات، الأرشيف، سياسة الإيماءات.
- `media`: محرك Media3 وMetadata وthumbnail loader وخدمة MediaSession.
- `ui`: شاشات Compose وViewModel وحالة موحدة.

## تدفق التصفح

`ExplorerViewModel` يطلب البيانات من `FileRepository` في `Dispatchers.IO`، ثم يعرض `StateFlow` غير قابل للتعديل. لا توجد عمليات I/O داخل Composable. Jobs البحث والتحليل والتصفح تُلغى عند بدء عملية أحدث.

## أغلفة المجلدات والصور المصغرة

`FolderCoverResolver` يبحث محليًا بالترتيب `cover` ثم `poster` ثم `folder`، ثم يختار أول صورة مناسبة كبديل. لا يوجد فحص عودي عند فتح كل بطاقة. `ThumbnailProvider` يهيئ Coil مع VideoFrameDecoder وMemory/Disk Cache محدودين، وتستخدمه بطاقات الملفات.

## العمليات الخطرة

`FileOperationManager` ينفذ النسخ والنقل في الخلفية مع تعارضات آمنة وإشارات تقدم، ويمنع نسخ/نقل المجلد إلى نفسه. الحذف المحلي ينقل العنصر إلى مجلد سلة المحذوفات مع حفظ المسار الأصلي بترميز آمن. SAF يعرض القراءة، بينما العمليات غير المدعومة تعرض رسالة واضحة بدل تنفيذ حذف دائم صامت.

## الوسائط

`MediaEngine` يغلف ExoPlayer ويحتفظ بحالة التشغيل والموضع وقائمة التشغيل. `MediaScreen` يوفر عارض صور قابلًا للتكبير والسحب، معرضًا أفقيًا، فيديو، صوتًا، إيماءات، وملء الشاشة. توجد `ExplorerPlaybackService` كبنية MediaSession، لكن ربطها بمحرك الواجهة والتحقق من إشعار الخلفية يحتاج جهازًا فعليًا.

## الشبكة والسحابة

`RemoteFileSystem` و`CloudProvider` عقود توسعة فقط. لا يوجد مزود SMB أو FTP أو WebDAV أو OAuth فعلي في هذا الإصدار، ولا تُعرض هذه المزايا على أنها مكتملة.

## التحقق

التفاصيل الدقيقة في `TESTING.md` و`IMPLEMENTATION_STATUS.md`. آخر Build/Lint/Unit Test ناجح في CI هو run `34797004649`، وmetadata الـAPK في run `34797339729`.
