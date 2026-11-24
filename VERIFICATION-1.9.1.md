# Verification — Compiler Studio 1.9.1

## Compiler/runtime tests

- Frontend sources compiled with Kotlin/JVM against `app/libs/compiler-engine.jar`.
- All test sources compiled.
- Test groups: 12 engine + 13 indentation frontend + 17 function frontend + 4 source-map = 46 methods.
- Result: **46 passed, 0 failed**.
- The harness supplied the JUnit `@Test` annotation and assertion methods locally because Maven dependencies could not be downloaded in this environment. Test bodies and assertions were the project's own test sources.

## Android packaging

- Android Gradle Plugin: 8.9.2; Gradle distribution: 8.11.1; Kotlin: 2.0.21; JDK target: 17; compileSdk: 35.
- An APK was **not** produced here: no Android SDK / `android.jar` or installed Gradle distribution is present, and external dependency resolution is unavailable.
- Build from Android Studio using JDK 17 and the configured Gradle 8.11.1 distribution. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
