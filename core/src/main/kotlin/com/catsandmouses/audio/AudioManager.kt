package com.catsandmouses.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.catsandmouses.Constants
import com.catsandmouses.ProgressStore

/**
 * Единственная точка звука в игре: музыка (заставка/меню/геймплей) и SFX.
 *
 * Все методы — безопасный no-op, если аудио недоступно (`Gdx.audio == null`):
 * так headless-тесты и dev-сборки без звука не падают. Ассеты — OGG Vorbis
 * (`assets/audio/...`). Меню и Настройки делят один трек (повторный `playMusic`
 * с тем же треком ничего не делает — музыка не перезапускается).
 */
class AudioManager(private val progress: ProgressStore) {

    companion object {
        const val TRACK_MENU = "menu"
        const val TRACK_GAMEPLAY = "gameplay"

        private val TRACK_FILES = mapOf(
            TRACK_MENU to "audio/music/menu.ogg",
            TRACK_GAMEPLAY to "audio/music/gameplay.ogg"
        )
    }

    private val musics = HashMap<String, Music>()
    private val sounds = HashMap<String, Sound>()

    private var currentTrack: String? = null
    private var ducked = false
    private var musicVolume = progress.musicVolume() / 100f
    private var sfxVolume = progress.sfxVolume() / 100f

    /** Играть трек (по умолчанию зацикленно). Повторный вызов с тем же треком — no-op. */
    fun playMusic(track: String, loop: Boolean = true) {
        val audio = Gdx.audio ?: return
        if (currentTrack == track) return
        stopMusic()
        val file = TRACK_FILES[track] ?: return
        val music = musics.getOrPut(track) { audio.newMusic(Gdx.files.internal(file)) }
        music.isLooping = loop
        music.volume = effectiveMusicVolume()
        music.play()
        currentTrack = track
    }

    fun stopMusic() {
        val track = currentTrack ?: return
        musics[track]?.let { if (it.isPlaying) it.stop() }
        currentTrack = null
        ducked = false
    }

    /** Пауза музыки при сворачивании приложения (Android/Desktop lifecycle). */
    fun pauseMusic() {
        currentTrack?.let { musics[it]?.pause() }
    }

    fun resumeMusic() {
        currentTrack?.let { musics[it]?.play() }
    }

    /** Приглушение музыки (например, пауза в игре). */
    fun setDuck(value: Boolean) {
        if (ducked == value) return
        ducked = value
        currentTrack?.let { musics[it]?.volume = effectiveMusicVolume() }
    }

    fun setMusicVolume(percent: Int) {
        musicVolume = percent.coerceIn(0, 100) / 100f
        currentTrack?.let { musics[it]?.volume = effectiveMusicVolume() }
    }

    fun setSfxVolume(percent: Int) {
        sfxVolume = percent.coerceIn(0, 100) / 100f
    }

    /** Проиграть SFX по имени файла без расширения (`assets/audio/sfx/<name>.ogg`). */
    fun playSfx(name: String) {
        if (sfxVolume <= 0f) return
        val audio = Gdx.audio ?: return
        val sound = sounds.getOrPut(name) {
            audio.newSound(Gdx.files.internal("audio/sfx/$name.ogg"))
        }
        sound.play(sfxVolume)
    }

    private fun effectiveMusicVolume(): Float =
        musicVolume * (if (ducked) Constants.MUSIC_DUCK else 1f)

    fun dispose() {
        musics.values.forEach { it.dispose() }
        sounds.values.forEach { it.dispose() }
        musics.clear()
        sounds.clear()
        currentTrack = null
    }
}
