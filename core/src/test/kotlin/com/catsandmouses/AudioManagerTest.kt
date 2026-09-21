package com.catsandmouses

import com.catsandmouses.audio.AudioManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * AudioManager в headless-контексте: аудио недоступно (`Gdx.audio == null`),
 * поэтому любые вызовы обязаны быть безопасным no-op.
 */
class AudioManagerTest : GdxTestBase() {

    private fun manager() = AudioManager(ProgressStore(FakePreferences()))

    @Test
    fun `в тестовом окружении аудио недоступно`() {
        assertNull(com.badlogic.gdx.Gdx.audio)
    }

    @Test
    fun `без аудио все методы безопасны`() {
        val audio = manager()
        audio.playMusic(AudioManager.TRACK_MENU)
        audio.playMusic(AudioManager.TRACK_GAMEPLAY)
        audio.setDuck(true)
        audio.setDuck(false)
        audio.setMusicVolume(0)
        audio.setMusicVolume(100)
        audio.setSfxVolume(0)
        audio.playSfx("click")
        audio.playSfx("cheese")
        audio.pauseMusic()
        audio.resumeMusic()
        audio.stopMusic()
        audio.dispose()
    }

    @Test
    fun `имена треков стабильны`() {
        assertEquals("menu", AudioManager.TRACK_MENU)
        assertEquals("gameplay", AudioManager.TRACK_GAMEPLAY)
    }
}
