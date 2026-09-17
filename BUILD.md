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

تم تشغيل الأوامر منفصلة في CI run `35278719183` على كود التطبيق `e82ee10140351951befd545516bc4245983c96bb`:

- Unit tests: PASS، وعددها 29.
- Lint: PASS.
- assembleDebug: PASS.

تم التقاط metadata للـAPK في run `35279113727` لنفس كود التطبيق. حجم APK `24,018,899` بايت وSHA-256 هو `9a463bea5f3e212370439aeebb8cd0f63c008d86715551a8e42556045aaeffd9`.

## بيئة Arena المحلية

البيئة المحلية لا تحتوي `java` أو Android SDK، ولذلك تفشل أوامر Gradle محليًا قبل بدء Gradle برسالة عدم وجود Java. يستخدم Workflow الحالي JDK 17 ويثبت Android command-line tools وPlatform 35 وBuild Tools 35.0.0 مؤقتًا داخل runner.

## الصلاحيات والتخزين

يستخدم التطبيق صلاحيات الوسائط الحديثة عند الحاجة، ويعتمد على SAF لوحدات التخزين الخارجية. لا يستخدم `MANAGE_EXTERNAL_STORAGE` كحل لتجاوز Scoped Storage.
