package com.chintan992.xplayer.ui

import app.cash.turbine.test
import com.chintan992.xplayer.PlayerPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SettingsViewModel
 * Verifies that the ViewModel correctly exposes preferences and updates them
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var repository: PlayerPreferencesRepository
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        
        // Setup default flows
        coEvery { repository.defaultPlayerType } returns flowOf("EXO")
        coEvery { repository.defaultOrientation } returns flowOf(true)
        coEvery { repository.defaultSpeed } returns flowOf(1.0f)
        coEvery { repository.defaultAspectRatio } returns flowOf("FIT")
        coEvery { repository.defaultDecoder } returns flowOf("AUTO")
        coEvery { repository.autoPlayNext } returns flowOf(true)
        coEvery { repository.seekDuration } returns flowOf(10)
        coEvery { repository.longPressSpeed } returns flowOf(2.0f)
        coEvery { repository.controlsTimeout } returns flowOf(3000)
        coEvery { repository.resumePlayback } returns flowOf(true)
        coEvery { repository.keepScreenOn } returns flowOf(true)
        
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `defaultPlayerType flow emits EXO as default`() = runTest {
        viewModel.defaultPlayerType.test {
            assertEquals("EXO", awaitItem())
        }
    }

    @Test
    fun `defaultOrientation flow emits true (landscape) as default`() = runTest {
        viewModel.defaultOrientation.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `defaultSpeed flow emits 1f as default`() = runTest {
        viewModel.defaultSpeed.test {
            assertEquals(1.0f, awaitItem())
        }
    }

    @Test
    fun `defaultAspectRatio flow emits FIT as default`() = runTest {
        viewModel.defaultAspectRatio.test {
            assertEquals("FIT", awaitItem())
        }
    }

    @Test
    fun `defaultDecoder flow emits AUTO as default`() = runTest {
        viewModel.defaultDecoder.test {
            assertEquals("AUTO", awaitItem())
        }
    }

    @Test
    fun `seekDuration flow emits 10 as default`() = runTest {
        viewModel.seekDuration.test {
            assertEquals(10, awaitItem())
        }
    }

    @Test
    fun `longPressSpeed flow emits 2f as default`() = runTest {
        viewModel.longPressSpeed.test {
            assertEquals(2.0f, awaitItem())
        }
    }

    @Test
    fun `controlsTimeout flow emits 3000 as default`() = runTest {
        viewModel.controlsTimeout.test {
            assertEquals(3000, awaitItem())
        }
    }

    @Test
    fun `autoPlayNext flow emits true as default`() = runTest {
        viewModel.autoPlayNext.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `resumePlayback flow emits true as default`() = runTest {
        viewModel.resumePlayback.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `keepScreenOn flow emits true as default`() = runTest {
        viewModel.keepScreenOn.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `updateDefaultOrientation calls repository`() = runTest {
        viewModel.updateDefaultOrientation(false)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateDefaultOrientation(false) }
    }

    @Test
    fun `updateDefaultSpeed calls repository`() = runTest {
        viewModel.updateDefaultSpeed(1.5f)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateDefaultSpeed(1.5f) }
    }

    @Test
    fun `updateDefaultAspectRatio calls repository`() = runTest {
        viewModel.updateDefaultAspectRatio("FILL")
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateDefaultAspectRatio("FILL") }
    }

    @Test
    fun `updateDefaultDecoder calls repository`() = runTest {
        viewModel.updateDefaultDecoder("HW")
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateDefaultDecoder("HW") }
    }

    @Test
    fun `updateSeekDuration calls repository`() = runTest {
        viewModel.updateSeekDuration(30)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateSeekDuration(30) }
    }

    @Test
    fun `updateLongPressSpeed calls repository`() = runTest {
        viewModel.updateLongPressSpeed(3.0f)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateLongPressSpeed(3.0f) }
    }

    @Test
    fun `updateControlsTimeout calls repository`() = runTest {
        viewModel.updateControlsTimeout(5000)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateControlsTimeout(5000) }
    }

    @Test
    fun `updateAutoPlayNext calls repository`() = runTest {
        viewModel.updateAutoPlayNext(false)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateAutoPlayNext(false) }
    }

    @Test
    fun `updateResumePlayback calls repository`() = runTest {
        viewModel.updateResumePlayback(false)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateResumePlayback(false) }
    }

    @Test
    fun `updateKeepScreenOn calls repository`() = runTest {
        viewModel.updateKeepScreenOn(false)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.updateKeepScreenOn(false) }
    }
}
