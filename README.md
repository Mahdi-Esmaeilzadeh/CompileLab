# CompileLab

**کامپایلر را مرحله‌به‌مرحله ببینید!**

CompileLab یک اپلیکیشن آموزشی اندروید است که عملکرد کامپایلر را در قالب شش مرحله به‌صورت تصویری نمایش می‌دهد.

کد شبه‌پایتون خود را بنویسید و با فشردن دکمه اجرا، مراحل مختلف کامپایل، از تحلیل لغوی تا تولید بایت‌کد و اجرای برنامه را مشاهده کنید.

این برنامه کاملاً آفلاین است و از زبان‌های فارسی و انگلیسی پشتیبانی می‌کند.

---

## دانلود

**آخرین نسخه: 1.10.1**

[دانلود CompileLab (APK)](../../releases/latest)

حداقل نسخه موردنیاز: Android 8.0

---

## قابلیت‌ها

- **Lexer:** نمایش توکن‌ها در قالب جدول.
- **Parser:** ساخت و نمایش درخت نحوی (AST).
- **Semantic Analysis:** بررسی متغیرها و نوع داده‌ها.
- **Intermediate Code:** تولید کد میانی (TAC).
- **Optimization:** بهینه‌سازی کد میانی.
- **Bytecode & VM:** تولید بایت‌کد و اجرای برنامه.
- **مدیریت خطا:** نمایش خطاها همراه با شماره خط، توضیح و راهنمای اصلاح.
- **رابط کاربری دوزبانه:** فارسی و انگلیسی با پشتیبانی از RTL.
- **ابزارهای ویرایش کد:** کپی، جای‌گذاری و پاک‌کردن کد.
- **کاملاً آفلاین:** کدهای شما از دستگاه خارج نمی‌شوند.

---

## زبان پشتیبانی‌شده

CompileLab از یک زیرمجموعه آموزشی الهام‌گرفته از Python پشتیبانی می‌کند.

قابلیت‌های اصلی:

- متغیرها و انواع داده پایه
- دستورات `print`
- عملگرهای ریاضی، مقایسه‌ای و منطقی
- شرط‌های `if`، `elif` و `else`
- حلقه‌های `while` و `for`
- توابع ساده
- توضیحات تک‌خطی با `#`

**توجه:** این زبان Python واقعی نیست و از تمام قابلیت‌های آن پشتیبانی نمی‌کند.

### نمونه کد

```python
total = 0

for i in range(1, 6):
    total = total + i

print(total)
```

خروجی:

```text
15
```

---

## نحوه عملکرد

```text
Source Code
    |
    v
  Lexer
    |
    v
  Parser
    |
    v
Semantic Analysis
    |
    v
Intermediate Code
    |
    v
Optimization
    |
    v
Bytecode & VM
    |
    v
  Output
```

---

## ساخت پروژه از سورس

### پیش‌نیازها

- Android Studio
- JDK 17 یا جدیدتر
- Android SDK Platform 35

### اجرا

پروژه را در Android Studio باز کنید و منتظر تکمیل Gradle Sync بمانید.

برای ساخت APK در ویندوز:

```powershell
.\gradlew.bat :app:assembleDebug
```

مسیر خروجی:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### اجرای تست‌ها

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

---

## فناوری‌ها

| فناوری | نسخه |
|---|---|
| Kotlin | 2.0.21 |
| Jetpack Compose BOM | 2025.02.00 |
| Android Gradle Plugin | 8.9.2 |
| Gradle | 8.11.1 |
| Min Android SDK | 26 |
| Target SDK | 35 |

---

## ساختار پروژه

```text
app/
├── src/main/java/com/mahdi/compilerstudio/
│   ├── MainActivity.kt
│   ├── Ui.kt
│   ├── Pipeline.kt
│   ├── IndentationFrontend.kt
│   ├── FunctionFrontend.kt
│   ├── ErrorExplainer.kt
│   ├── StageTables.kt
│   ├── AstTree.kt
│   ├── StageInfo.kt
│   ├── Examples.kt
│   ├── Report.kt
│   └── Lang.kt
└── libs/
    └── compiler-engine.jar
```

---

## مشارکت

گزارش مشکلات، پیشنهاد قابلیت‌های جدید و Pull Requestها مورد استقبال هستند.

---

## مجوز

اطلاعات مجوز پروژه متعاقباً اضافه خواهد شد.
