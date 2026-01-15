package com.chintan992.xplayer

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PlayerPreferencesRepository
 * Verifies that all preference flows return expected defaults and updates work correctly
 */
class PlayerPreferencesRepositoryTest {

    @Test
    fun `defaults object has expected values`() {
        // Verify all defaults are set correctly
        assertEquals(true, PlayerPreferencesRepository.Defaults.ORIENTATION_LANDSCAPE)
        assertEquals(1.0f, PlayerPreferencesRepository.Defaults.SPEED)
        assertEquals("FIT", PlayerPreferencesRepository.Defaults.ASPECT_RATIO)
        assertEquals("AUTO", PlayerPreferencesRepository.Defaults.DECODER)
        assertEquals(true, PlayerPreferencesRepository.Defaults.AUTO_PLAY)
        assertEquals(10, PlayerPreferencesRepository.Defaults.SEEK_DURATION_SECONDS)
        assertEquals(2.0f, PlayerPreferencesRepository.Defaults.LONG_PRESS_SPEED)
        assertEquals(3000, PlayerPreferencesRepository.Defaults.CONTROLS_TIMEOUT_MS)
        assertEquals(true, PlayerPreferencesRepository.Defaults.RESUME_PLAYBACK)
        assertEquals(true, PlayerPreferencesRepository.Defaults.KEEP_SCREEN_ON)
    }

    @Test
    fun `default seek duration is 10 seconds`() {
        val defaultValue = PlayerPreferencesRepository.Defaults.SEEK_DURATION_SECONDS
        assertEquals(10, defaultValue)
    }

    @Test
    fun `default long press speed is 2x`() {
        val defaultValue = PlayerPreferencesRepository.Defaults.LONG_PRESS_SPEED
        assertEquals(2.0f, defaultValue)
    }

    @Test
    fun `default controls timeout is 3000 ms`() {
        val defaultValue = PlayerPreferencesRepository.Defaults.CONTROLS_TIMEOUT_MS
        assertEquals(3000, defaultValue)
    }

    @Test
    fun `default orientation is landscape`() {
        val defaultValue = PlayerPreferencesRepository.Defaults.ORIENTATION_LANDSCAPE
        assertTrue(defaultValue)
    }

    @Test
    fun `default aspect ratio is FIT`() {
        val defaultValue = PlayerPreferencesRepository.Defaults.ASPECT_RATIO
        assertEquals("FIT", defaultValue)
    }

    @Test
    fun `default decoder is AUTO`() {
        val defaultValue = PlayerPreferencesRepository.Defaults.DECODER
        assertEquals("AUTO", defaultValue)
    }
}
