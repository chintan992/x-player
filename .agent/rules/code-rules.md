---
trigger: always_on
---

1.  **Media3 Only:** Do not use `com.google.android.exoplayer2.*`. Use `androidx.media3.*`.
2.  **No Magic Numbers:** All constants (timeouts, buffer sizes) must be defined in a `Config` object.
3.  **Error Handling:** Every network call or file access must be wrapped in `try-catch` blocks that emit proper error states to the UI.
4.  **Java Compatibility:** While the app is Kotlin, ensure the Hilt modules are set up correctly to inject into Android Service classes (which have different lifecycles).
5.  **Strict Mode:** If you use `UnstableApi` (common in Media3), strictly annotate the method or class with `@OptIn(UnstableApi::class)` to prevent build errors.