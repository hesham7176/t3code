# البناء والتحقق

## المتطلبات

- JDK 17
- Android SDK Platform 35 وBuild Tools مناسبة
- Gradle 8.7 أو Android Studio حديث

## الأوامر

```bash
gradle assembleDebug
gradle test
gradle lint
```

## الصلاحيات

يستخدم التطبيق الصلاحيات الحديثة الخاصة بالصور والفيديو والصوت عند الحاجة، ويعتمد على SAF للوصول إلى وحدات التخزين التي لا يملك لها مسارًا مباشرًا. لا يستخدم `MANAGE_EXTERNAL_STORAGE` كحل التفافي.

## ملاحظة بيئة التطوير

إذا لم يكن Gradle أو Android SDK مثبتًا في بيئة التشغيل، يمكن فتح المشروع في Android Studio أو استخدام CI الموجود في `.github/workflows/android.yml`.
