# Media Explorer

تطبيق Android أصلي لإدارة الملفات وتشغيل الوسائط من مكان واحد، مع دعم العربية والإنجليزية وواجهات RTL.

## حالة التحقق

تم التحقق آليًا بنجاح في GitHub Actions:

- `./gradlew testDebugUnitTest` — ناجح، 12 اختبارًا.
- `./gradlew lintDebug` — ناجح.
- `./gradlew assembleDebug` — ناجح.
- Gradle 8.7 وJDK 17.0.20.1 وAndroid SDK 35.
- APK: `app/build/outputs/apk/debug/app-debug.apk`، حجمه 23,986,603 بايت في CI.

هذا لا يعني اكتمال اختبار الجهاز؛ راجع `IMPLEMENTATION_STATUS.md` و`TESTING.md` للقيود الفعلية.

## مبادئ التصميم

- Kotlin وJetpack Compose وMaterial 3.
- فصل واجهة المستخدم عن منطق الملفات ومحرك الوسائط.
- استخدام coroutines للعمليات الثقيلة مع الإلغاء.
- استخدام Storage Access Framework لوحدات التخزين القابلة للإزالة.
- عدم حذف البيانات نهائيًا دون المرور بسلة المحذوفات في التخزين المحلي المدعوم.
- عدم الإعلان عن صيغة أو مزود غير مدعوم فعليًا.

راجع `ARCHITECTURE.md` و`ROADMAP.md` و`IMPLEMENTATION_STATUS.md` لمعرفة التفاصيل.
