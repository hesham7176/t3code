# البناء والتحقق

## المتطلبات

- JDK 17
- Android SDK Platform 35 وBuild Tools 35.0.0
- Gradle Wrapper الموجود في المستودع، ويستخدم Gradle 8.7

## استخدام الـ Wrapper

```bash
./gradlew --version
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

الناتج المتوقع:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## التحقق المؤكد

تم تشغيل الأوامر في GitHub Actions بنجاح في run النظيف `34797004649` على كود التطبيق `b334c82`. والتقط run `34797339729` metadata التفصيلي لنفس كود التطبيق؛ حجم APK كان `23,986,603` بايت.

## بيئة Arena المحلية

البيئة المحلية الحالية لا تحتوي `java` أو `gradle` أو Android SDK، لذلك كان تشغيل `./gradlew` محليًا يتوقف قبل بدء Gradle برسالة عدم وجود Java. استخدمت CI ببيئة JDK 17 وAndroid SDK 35 لإجراء التحقق الفعلي.

## الصلاحيات

يستخدم التطبيق الصلاحيات الحديثة الخاصة بالصور والفيديو والصوت عند الحاجة، ويعتمد على SAF للوصول إلى وحدات التخزين التي لا يملك لها مسارًا مباشرًا. لا يستخدم `MANAGE_EXTERNAL_STORAGE` كحل التفافي.
