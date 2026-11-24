# راهنمای اجرای پروژه و ساخت APK

## پیش‌نیاز

- Android Studio به‌روز
- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools سازگار با SDK 35
- دسترسی شبکه برای دریافت وابستگی‌های Gradle در اولین Sync، مگر اینکه قبلاً cache شده باشند

## مراحل

1. ZIP را Extract کنید.
2. پوشه‌ای را در Android Studio باز کنید که فایل `settings.gradle.kts` در ریشهٔ آن است.
3. Gradle JDK را روی JDK 17 بگذارید.
4. منتظر بمانید Gradle Sync بدون خطا تمام شود.
5. برای اجرای مستقیم، شبیه‌ساز یا گوشی متصل را انتخاب کنید و Run را بزنید.
6. برای APK از مسیر **Build > Build Bundle(s) / APK(s) > Build APK(s)** استفاده کنید.
7. فایل Debug APK معمولاً در `app/build/outputs/apk/debug/app-debug.apk` ساخته می‌شود.

## اگر Gradle Sync خطا داد

- مطمئن شوید Gradle JDK برابر 17 است، نه JRE یا JDK قدیمی.
- در تنظیمات Gradle، نسخهٔ توزیع را روی Gradle 8.11.1 قرار دهید. اگر Android Studio گزینهٔ Local Gradle distribution را نشان می‌دهد، مسیر نصب Gradle 8.11.1 را انتخاب کنید؛ در غیر این صورت اجازه دهید Android Studio آن را دریافت کند.
- مطمئن شوید SDK Platform 35 نصب است.
- اگر خطا مربوط به دانلود dependency است، اتصال اینترنت و تنظیمات proxy را بررسی و Sync را دوباره اجرا کنید.

## توجه دربارهٔ Wrapper

این بسته فایل تنظیمات `gradle/wrapper/gradle-wrapper.properties` را دارد، اما فایل اجرایی Wrapper (`gradlew`, `gradlew.bat` و `gradle-wrapper.jar`) در آرشیو مبدأ وجود نداشت؛ بنابراین آن‌ها را به‌صورت ساختگی اضافه نکرده‌ام. باز کردن پروژه از طریق Android Studio و انتخاب توزیع Gradle محلی/دانلودشده مسیر پیشنهادی است. در صورت نصب بودن Gradle 8.11.1 می‌توانید فرمان `gradle.bat :app:assembleDebug` را در ویندوز اجرا کنید.
