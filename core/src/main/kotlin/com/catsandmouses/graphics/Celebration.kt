package com.catsandmouses.graphics

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Праздник после финала — салют (фейерверки) и шарики, поднимающиеся снизу.
 * Чистая логика (без Gdx): положение, фазы и время частиц; отрисовка — в `GameScreen` (ShapeRenderer).
 * Детерминирована по seed (для headless-тестов), но в игре seed случайный.
 */
class Celebration(
    private val duration: Float = DEFAULT_DURATION,
    seed: Long = Random.nextLong()
) {

    /** Летящий снизу шарик. */
    class Balloon(
        var x: Float,
        var y: Float,
        val speed: Float,
        val swayAmp: Float,
        val swayFreq: Float,
        val size: Float,
        val colorIndex: Int,
        var phase: Float
    )

    /** Искра салюта (частица разлёта). */
    class Spark(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var age: Float,
        val life: Float,
        val colorIndex: Int
    )

    val balloons = mutableListOf<Balloon>()
    val sparks = mutableListOf<Spark>()

    private var elapsed = 0f
    private var balloonTimer = 0f
    private var burstTimer = 0f
    private val rnd = Random(seed)

    /** Праздник завершён — можно запускать титры. */
    val isFinished: Boolean get() = elapsed >= duration

    /** Прогресс 0..1 (для плавного появления/затухания). */
    val progress: Float get() = (elapsed / duration).coerceIn(0f, 1f)

    fun reset() {
        balloons.clear()
        sparks.clear()
        elapsed = 0f
        balloonTimer = 0f
        burstTimer = 0f
    }

    fun update(delta: Float) {
        elapsed += delta

        // Шарики снизу — периодический запуск
        balloonTimer -= delta
        if (balloonTimer <= 0f) {
            balloonTimer = BALLOON_INTERVAL * (0.6f + rnd.nextFloat() * 0.8f)
            balloons += Balloon(
                x = rnd.nextFloat() * WORLD_WIDTH,
                y = -BALLOON_MAX_SIZE,
                speed = BALLOON_SPEED_MIN + rnd.nextFloat() * (BALLOON_SPEED_MAX - BALLOON_SPEED_MIN),
                swayAmp = 18f + rnd.nextFloat() * 42f,
                swayFreq = 0.8f + rnd.nextFloat() * 1.4f,
                size = BALLOON_MIN_SIZE + rnd.nextFloat() * (BALLOON_MAX_SIZE - BALLOON_MIN_SIZE),
                colorIndex = rnd.nextInt(PALETTE_SIZE),
                phase = rnd.nextFloat() * TWO_PI
            )
        }

        // Салют — периодические вспышки
        burstTimer -= delta
        if (burstTimer <= 0f) {
            burstTimer = BURST_INTERVAL * (0.7f + rnd.nextFloat() * 0.6f)
            spawnBurst()
        }

        // Движение шариков
        val bi = balloons.iterator()
        while (bi.hasNext()) {
            val b = bi.next()
            b.y += b.speed * delta
            b.phase += b.swayFreq * delta
            if (b.y - b.size > WORLD_HEIGHT + 40f) bi.remove()
        }

        // Движение искр (возраст + гравитация)
        val si = sparks.iterator()
        while (si.hasNext()) {
            val s = si.next()
            s.age += delta
            if (s.age >= s.life) {
                si.remove()
                continue
            }
            s.x += s.vx * delta
            s.y += s.vy * delta
            s.vy -= SPARK_GRAVITY * delta
        }
    }

    /** Текущее горизонтальное смещение шарика от покачивания. */
    fun sway(b: Balloon): Float = sin(b.phase) * b.swayAmp

    /** Яркость искры 0..1 (линейное затухание к концу жизни). */
    fun sparkAlpha(s: Spark): Float = 1f - (s.age / s.life).coerceIn(0f, 1f)

    private fun spawnBurst() {
        val cx = WORLD_WIDTH * (0.12f + rnd.nextFloat() * 0.76f)
        val cy = WORLD_HEIGHT * (0.45f + rnd.nextFloat() * 0.45f)
        val count = BURST_MIN + rnd.nextInt(BURST_MAX - BURST_MIN + 1)
        val colorIndex = rnd.nextInt(PALETTE_SIZE)
        val baseSpeed = BURST_SPEED_MIN + rnd.nextFloat() * (BURST_SPEED_MAX - BURST_SPEED_MIN)
        for (i in 0 until count) {
            val angle = i.toFloat() / count * TWO_PI + (rnd.nextFloat() - 0.5f) * 0.25f
            val speed = baseSpeed * (0.6f + rnd.nextFloat() * 0.6f)
            sparks += Spark(
                x = cx,
                y = cy,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                age = 0f,
                life = SPARK_LIFE * (0.7f + rnd.nextFloat() * 0.6f),
                colorIndex = colorIndex
            )
        }
    }

    companion object {
        const val WORLD_WIDTH = 1280f
        const val WORLD_HEIGHT = 720f

        /** Длительность праздника до запуска титров, сек. */
        const val DEFAULT_DURATION = 5f

        /** Число цветов в палитре праздника (индекс → цвет задаёт рендер). */
        const val PALETTE_SIZE = 7

        private const val TWO_PI = 6.2831853f

        private const val BALLOON_INTERVAL = 0.14f
        private const val BALLOON_SPEED_MIN = 70f
        private const val BALLOON_SPEED_MAX = 130f
        private const val BALLOON_MIN_SIZE = 40f
        private const val BALLOON_MAX_SIZE = 64f

        private const val BURST_INTERVAL = 0.3f
        private const val BURST_MIN = 18
        private const val BURST_MAX = 30
        private const val BURST_SPEED_MIN = 110f
        private const val BURST_SPEED_MAX = 220f
        private const val SPARK_LIFE = 0.9f
        private const val SPARK_GRAVITY = 40f
    }
}
