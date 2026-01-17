package com.chintan992.xplayer.extractor

import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.extractor.flv.FlvExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.wav.WavExtractor
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.ts.PsExtractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.ts.Ac3Extractor
import androidx.media3.extractor.ts.Ac4Extractor

class CustomExtractorsFactory : ExtractorsFactory {
    override fun createExtractors(): Array<Extractor> {
        val extractors = mutableListOf<Extractor>()
        
        // Priority: Standard MatroskaExtractor
        extractors.add(MatroskaExtractor(androidx.media3.extractor.text.DefaultSubtitleParserFactory()))
        
        // Add other default extractors
        extractors.add(Mp4Extractor(androidx.media3.extractor.text.DefaultSubtitleParserFactory()))
        extractors.add(FragmentedMp4Extractor(androidx.media3.extractor.text.DefaultSubtitleParserFactory()))
        extractors.add(TsExtractor(androidx.media3.extractor.text.DefaultSubtitleParserFactory()))
        extractors.add(FlvExtractor())
        extractors.add(Mp3Extractor())
        extractors.add(AmrExtractor())
        extractors.add(WavExtractor())
        extractors.add(OggExtractor())
        extractors.add(PsExtractor())
        extractors.add(AdtsExtractor())
        extractors.add(Ac3Extractor())
        extractors.add(Ac4Extractor())
        
        return extractors.toTypedArray()
    }
}
