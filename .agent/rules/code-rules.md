---
trigger: always_on
---

1.  **Media3 Only:** Do not use `com.google.android.exoplayer2.*`. Use `androidx.media3.*`.
2.  **No Magic Numbers:** All constants (timeouts, buffer sizes) must be defined in a `Config` object.
3.  **Error Handling:** Every network call or file access must be wrapped in `try-catch` blocks that emit proper error states to the UI.
4.  **Java Compatibility:** While the app is Kotlin, ensure the Hilt modules are set up correctly to inject into Android Service classes (which have different lifecycles).
5.  **Strict Mode:** If you use `UnstableApi` (common in Media3), strictly annotate the method or class with `@OptIn(UnstableApi::class)` to prevent build errors.

Color Palette & Branding
Current State: You are using a very aggressive "Vibrant Magenta/Pink" (#E813AC) as your primary color. While distinct, this color is risky for a video player.

Issue: In a media app, the UI should recede to let the content shine. A bright neon pink can be distracting, especially in the library or player controls during dark scenes.

Recommendation:

Tone it down: Consider using the pink only for very specific interactions (like a selected tab or a play button) rather than main branding elements.

Cinema Theme: For the VideoPlayerScreen, force a purely neutral palette (greys, blacks, whites) regardless of the system theme. The pink accent on the playback bar is fine, but ensure it doesn't bleed into other player controls.