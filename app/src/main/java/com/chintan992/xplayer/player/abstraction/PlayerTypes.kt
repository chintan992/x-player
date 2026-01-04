package com.chintan992.xplayer.player.abstraction

enum class AspectRatioMode {
    FIT, FILL, ZOOM, STRETCH, RATIO_16_9, RATIO_4_3;

    val displayName: String
        get() = when (this) {
            FIT -> "Fit"
            FILL -> "Fill"
            ZOOM -> "Zoom"
            STRETCH -> "Stretch"
            RATIO_16_9 -> "16:9"
            RATIO_4_3 -> "4:3"
        }
}

enum class DecoderMode {
    HARDWARE, SOFTWARE, HARDWARE_STRICT, AUTO
}

data class TrackInfo(
    val index: Int,
    val groupIndex: Int,
    val name: String,
    val language: String?,
    val isSelected: Boolean,
    val isSupported: Boolean = true
) {
    val displayName: String
        get() {
            return if (name.isNotEmpty()) name
            else if (language != null) java.util.Locale(language).displayLanguage
            else "Unknown"
        }
}

enum class PlayerType {
    EXO, MPV
}
