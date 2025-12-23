package com.chintan992.xplayer

object PlaylistManager {
    var currentPlaylist: List<VideoItem> = emptyList()
    var currentVideoIndex: Int = 0
    
    fun setPlaylist(playlist: List<VideoItem>, startIndex: Int) {
        currentPlaylist = playlist
        currentVideoIndex = startIndex
    }
    
    fun clearPlaylist() {
        currentPlaylist = emptyList()
        currentVideoIndex = 0
    }
    
    fun getCurrentVideo(): VideoItem? {
        if (currentPlaylist.isNotEmpty() && currentVideoIndex in currentPlaylist.indices) {
            return currentPlaylist[currentVideoIndex]
        }
        return null
    }
}
