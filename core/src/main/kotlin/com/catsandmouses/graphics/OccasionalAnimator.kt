package com.catsandmouses.graphics

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Трансформ отрисовки: масштаб, поворот (градусы) и вертикальное смещение. */
data class ActTransform(
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val offsetY: Float = 0f
) {
    val isIdentity: Boolean get() = scale == 1f && rotation == 0f && offsetY == 0f

    companion object {
        val IDENTITY = ActTransform()
    }
}

/**
 * Один акцентный акт: длительность и кривая эффекта по прогрессу `t` ∈ [0, 1].
 * На границах (t = 0 и t = 1) акт всегда даёт identity — вход и выход без рывка.
 */
class Act(val duration: Float, private val effect: (Float) -> ActTransform) {

    init {
        require(duration > 0f) { "duration должен быть > 0" }
    }

    fun transformAt(t: Float): ActTransform {
        if (t <= 0f || t >= 1f) return ActTransform.IDENTITY
        return effect(t)
    }
}

/**
 * «Изредка-акцент» (idle-оживление): объект обычно статичен, но по случайному интервалу
 * [intervalMin, intervalMax] проигрывает один случайный акт из набора и возвращается в статику.
 *
 * Чистая логика без GL, детерминирована по seed — тестируется headless.
 * Время ведёт вызывающая сторона (`update(delta)`), поэтому анимацию можно замораживать.
 */
class OccasionalAnimator(
    private val acts: List<Act>,
    private val intervalMin: Float,
    private val intervalMax: Float,
    seed: Long
) : IdleAnimator {

    private val random = Random(seed)
    private var timeToNext = 0f
    private var activeAct: Act? = null
    private var actTime = 0f
    private var lastActIndex = -1

    init {
        require(acts.isNotEmpty()) { "Нужен хотя бы один акт" }
        require(intervalMin > 0f) { "intervalMin должен быть > 0" }
        require(intervalMax >= intervalMin) { "intervalMax должен быть >= intervalMin" }
        reset()
    }

    /** Начать заново: новый интервал покоя, акт сброшен. */
    fun reset() {
        timeToNext = nextInterval()
        activeAct = null
        actTime = 0f
        lastActIndex = -1
    }

    fun hasActiveAct(): Boolean = activeAct != null

    override fun update(delta: Float) {
        if (delta <= 0f) return
        var remaining = delta
        while (remaining > 0f) {
            val act = activeAct
            if (act == null) {
                if (remaining < timeToNext) {
                    timeToNext -= remaining
                    remaining = 0f
                } else {
                    remaining -= timeToNext
                    activeAct = acts[nextActIndex()]
                    actTime = 0f
                }
            } else {
                val left = act.duration - actTime
                if (remaining < left) {
                    actTime += remaining
                    remaining = 0f
                } else {
                    remaining -= left
                    activeAct = null
                    actTime = 0f
                    timeToNext = nextInterval()
                }
            }
        }
    }

    /** Текущий трансформ: identity в покое, эффект акта — во время акта. */
    override fun transform(): ActTransform {
        val act = activeAct ?: return ActTransform.IDENTITY
        return act.transformAt(actTime / act.duration)
    }

    private fun nextInterval(): Float =
        if (intervalMax <= intervalMin) intervalMin
        else intervalMin + random.nextFloat() * (intervalMax - intervalMin)

    /** Индекс следующего акта; один и тот же акт не выпадает два раза подряд. */
    private fun nextActIndex(): Int {
        val n = acts.size
        if (n == 1) return 0
        var index = random.nextInt(n)
        if (index == lastActIndex) {
            index = (lastActIndex + 1 + random.nextInt(n - 1)) % n
        }
        lastActIndex = index
        return index
    }
}

/** Кривые акцентов, общие для типов (используются наборами актов вроде CheeseAnimation). */
object ActCurves {

    /** Колокол: 0 на краях, 1 в середине. */
    fun bell(t: Float): Float = sin(PI.toFloat() * t)

    /** Полное колебание: 0 на краях, размах ±1 (знак меняется). */
    fun oscillation(t: Float): Float = sin(2f * PI.toFloat() * t)

    /** Непрерывный цикл: 0 на краях, 1 в середине (для постоянных пульсаций). */
    fun pulse(t: Float): Float = 0.5f - 0.5f * cos(2f * PI.toFloat() * t)
}
