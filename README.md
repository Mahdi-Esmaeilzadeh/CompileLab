# Compiler Studio 1.9.1

پروژهٔ اندروید Compiler Studio برای باز کردن مستقیم در Android Studio آماده شده است. رابط کاربری به موتور کامپایلر متصل است و نتیجهٔ واقعی شش مرحلهٔ پردازش را نمایش می‌دهد.

## باز کردن و اجرا در Android Studio

1. فایل ZIP را کامل Extract کنید.
2. در Android Studio گزینهٔ **Open** را بزنید و پوشهٔ `CompilerStudio-AndroidStudio-1.9.1` را انتخاب کنید؛ همان پوشه‌ای که `settings.gradle.kts` داخل آن قرار دارد.
3. در مسیر **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**، مقدار **Gradle JDK** را روی **JDK 17** قرار دهید.
4. اجازه دهید Gradle Sync کامل شود. پروژه از Android Gradle Plugin `8.9.2` و Gradle `8.11.1` استفاده می‌کند.
5. یک شبیه‌ساز اجرا کنید یا گوشی را با USB debugging متصل کنید.
6. از نوار بالای Android Studio، پیکربندی `app` را انتخاب و دکمهٔ **Run** را بزنید.

## گرفتن فایل APK

از منوی Android Studio:

**Build > Build Bundle(s) / APK(s) > Build APK(s)**

پس از پایان موفق Build، روی **locate** کلیک کنید. مسیر معمول فایل خروجی:

`app/build/outputs/apk/debug/app-debug.apk`

همچنین از پنجرهٔ Terminal در ریشهٔ پروژه می‌توانید اجرا کنید:

- Windows: `gradle.bat :app:assembleDebug`
- macOS / Linux: `gradle :app:assembleDebug`

این فرمان به نصب Gradle 8.11.1 یا انتخاب توزیع محلی آن در Android Studio نیاز دارد.

## تنظیمات پروژه

- Android Gradle Plugin: 8.9.2
- Kotlin: 2.0.21
- Gradle: 8.11.1
- JDK: 17
- compileSdk / targetSdk: 35
- minSdk: 26
- applicationId: `com.mahdi.compilerstudio`
- versionName / versionCode: `1.9.1` / `11`

موتور کامپایلر در `app/libs/compiler-engine.jar` قرار دارد و به دریافت جداگانه نیاز ندارد. فایل `local.properties` عمداً داخل ZIP نیست؛ Android Studio آن را متناسب با مسیر SDK سیستم شما ایجاد می‌کند.

## قابلیت‌ها و محدودیت‌های زبان

این برنامه یک زیرمجموعهٔ آموزشی Python-like را اجرا می‌کند، نه تمام Python. جزئیات نحو پشتیبانی‌شده و محدودیت توابع در راهنمای داخل برنامه و `PROJECT_STATUS.md` آمده است. توابع در نسخهٔ فعلی هنگام کامپایل گسترش می‌یابند و هنوز Call Frame واقعی در VM ندارند.

## وضعیت اعتبارسنجی

- دو Frontend با Kotlin/JVM در محیط توسعه کامپایل شده‌اند.
- تست رگرسیون مستقیم موتور و Frontend با موفقیت اجرا شده است.
- مجموعهٔ ۴۶ تست موجود قبلاً در harness محلی اجرا شده و ۴۶ مورد موفق گزارش شده‌اند.
- APK این بسته در محیط سازنده تولید نشده است؛ بنابراین موفقیت Gradle Sync، ساخت APK و اجرای روی گوشی باید در Android Studio شما تأیید شود.
