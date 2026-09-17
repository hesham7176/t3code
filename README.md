# Media Explorer

تطبيق Android أصلي لإدارة الملفات وتشغيل الوسائط، مع دعم العربية والإنجليزية وواجهات RTL.

## آخر تحقق فعلي

تم التحقق من كود التطبيق في GitHub Actions:

- `./gradlew testDebugUnitTest` — ناجح، **29 اختبارًا**.
- `./gradlew lintDebug` — ناجح.
- `./gradlew assembleDebug` — ناجح.
- Gradle 8.7 وJDK 17.0.20.1 وAndroid SDK 35.
- CI run: `35278719183`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- حجم APK: `24,018,899` بايت.
- SHA-256: `9a463bea5f3e212370439aeebb8cd0f63c008d86715551a8e42556045aaeffd9`.

هذا تحقق آلي فقط. لم يتم تشغيل التطبيق على جهاز Android فعلي أو Emulator، ولا توجد تكاملات فعلية مع SMB أو FTP أو WebDAV أو Cloud.

## مبادئ التصميم

- Kotlin وJetpack Compose وMaterial 3.
- فصل UI وPresentation وDomain وData ومحرك الوسائط.
- Coroutines وFlow وViewModel للعمليات والحالة.
- استخدام Storage Access Framework للتخزين الذي لا يملك التطبيق له مسارًا مباشرًا.
- عدم اعتبار URI الخاص بـSAF مسار `java.io.File`.
- عدم حذف البيانات نهائيًا بصمت.
- عدم الإعلان عن مزود أو صيغة على أنها مدعومة دون تنفيذ فعلي.

راجع `IMPLEMENTATION_STATUS.md` للمصفوفة الدقيقة، و`TESTING.md` للقيود، و`ROADMAP.md` للخطوات المتبقية.
