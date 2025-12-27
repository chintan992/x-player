package com.chintan992.xplayer

object PlayerConfig {
    // Playback
    const val SEEK_INCREMENT_MS = 10_000L // 10 seconds

    const val MAX_VOLUME_BOOST = 2.0f
    
    // Picture-in-Picture
    // Android valid range is ~1/2.39 to 2.39/1
    const val PIP_ASPECT_RATIO_MIN = 0.41841004184f // ~1/2.39
    const val PIP_ASPECT_RATIO_MAX = 2.39000000000f
    const val PIP_DEFAULT_ASPECT_RATIO_NUMERATOR = 16
    const val PIP_DEFAULT_ASPECT_RATIO_DENOMINATOR = 9
}
