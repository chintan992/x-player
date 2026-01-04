package com.chintan992.xplayer.player.abstraction

data class TrackInfo(
    val index: Int,
    val groupIndex: Int,
    val name: String,
    val language: String?,
    val isSelected: Boolean
)

enum class AspectRatioMode(val displayName: String, val ratio: Int) {
    FIT(displayName = "Fit", ratio = 0),
    FILL(displayName = "Fill", ratio = 1),
    ZOOM(displayName = "Zoom", ratio = 2),
    STRETCH(displayName = "Stretch", ratio = 3),
    RATIO_16_9(displayName = "16:9", ratio = 4),
    RATIO_4_3(displayName = "4:3", ratio = 5)
}

enum class DecoderMode(val displayName: String) {
    HARDWARE(displayName = "Hardware (HW)"),
    SOFTWARE(displayName = "Software (SW)"),
    AUTO(displayName = "Auto")
}
