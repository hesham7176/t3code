# البناء والتحقق

## المتطلبات

- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0
- Gradle Wrapper الموجود في المستودع، ويستخدم Gradle 8.7

## أوامر التحقق

```bash
./gradlew --version
./gradlew testDebugUnitTest --stacktrace --console=plain
./gradlew lintDebug --stacktrace --console=plain
./gradlew assembleDebug --stacktrace --console=plain
```

الناتج:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## التحقق المؤكد

تم تشغيل الأوامر منفصلة في CI run `35280160901` على كود التطبيق `7ee03dcfa98b00326bf77d18cf53265c17c95a69`:

- Unit tests: PASS، وعددها 29.
- Lint: PASS.
- assembleDebug: PASS.

تم التقاط metadata للـAPK في run `35280589338` لنفس كود التطبيق. حجم APK `24,018,899` بايت وSHA-256 هو `4002cedbde51987d005a2e1597c2139dc70907b03b94b0e596f57363566c73ae`.

## بيئة Arena المحلية

البيئة المحلية لا تحتوي `java` أو Android SDK، ولذلك تفشل أوامر Gradle محليًا قبل بدء Gradle برسالة عدم وجود Java. يستخدم Workflow الحالي JDK 17 ويثبت Android command-line tools وPlatform 35 وBuild Tools 35.0.0 مؤقتًا داخل runner.

## الصلاحيات والتخزين

يستخدم التطبيق صلاحيات الوسائط الحديثة عند الحاجة، ويعتمد على SAF لوحدات التخزين الخارجية. لا يستخدم `MANAGE_EXTERNAL_STORAGE` كحل لتجاوز Scoped Storage.
