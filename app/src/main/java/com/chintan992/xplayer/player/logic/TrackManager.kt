package com.chintan992.xplayer.player.logic

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.chintan992.xplayer.TrackInfo
import com.chintan992.xplayer.data.SubtitleRepository
import com.chintan992.xplayer.data.SubtitleResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TrackManager @Inject constructor(
    private val subtitleRepository: SubtitleRepository
) {

    fun getTracks(player: ExoPlayer?): Pair<List<TrackInfo>, List<TrackInfo>> {
        val p = player ?: return Pair(emptyList(), emptyList())
        val tracks = p.currentTracks
        
        val audioTracks = mutableListOf<TrackInfo>()
        val subtitleTracks = mutableListOf<TrackInfo>()

        tracks.groups.forEachIndexed { groupIndex, group ->
            val trackType = group.type
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                
                val trackName = format.label 
                    ?: format.language?.uppercase() 
                    ?: "Track ${trackIndex + 1}"
                
                val trackInfo = TrackInfo(
                    index = trackIndex,
                    groupIndex = groupIndex,
                    name = trackName,
                    language = format.language,
                    isSelected = isSelected
                )

                when (trackType) {
                    C.TRACK_TYPE_AUDIO -> audioTracks.add(trackInfo)
                    C.TRACK_TYPE_TEXT -> subtitleTracks.add(trackInfo)
                }
            }
        }
        return Pair(audioTracks, subtitleTracks)
    }

    fun selectAudioTrack(player: ExoPlayer?, trackInfo: TrackInfo) {
        val p = player ?: return
        val tracks = p.currentTracks
        
        if (trackInfo.groupIndex < tracks.groups.size) {
            val group = tracks.groups[trackInfo.groupIndex]
            val override = TrackSelectionOverride(group.mediaTrackGroup, trackInfo.index)
            
            p.trackSelectionParameters = p.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(override)
                .build()
        }
    }

    fun selectSubtitleTrack(player: ExoPlayer?, trackInfo: TrackInfo?) {
        val p = player ?: return
        
        if (trackInfo == null) {
            // Disable subtitles
            p.trackSelectionParameters = p.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val tracks = p.currentTracks
            if (trackInfo.groupIndex < tracks.groups.size) {
                val group = tracks.groups[trackInfo.groupIndex]
                val override = TrackSelectionOverride(group.mediaTrackGroup, trackInfo.index)
                
                p.trackSelectionParameters = p.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(override)
                    .build()
            }
        }
    }

    fun searchSubtitles(query: String): Flow<List<SubtitleResult>> {
        return subtitleRepository.searchSubtitles(query)
    }

    suspend fun downloadSubtitle(url: String): android.net.Uri? {
        return subtitleRepository.downloadSubtitle(url)
    }
}
