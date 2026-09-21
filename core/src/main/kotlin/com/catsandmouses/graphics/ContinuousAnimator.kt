package com.catsandmouses.graphics

/**
 * Постоянный циклический акцент: фаза крутится непрерывно `(time + phase) % period`,
 * трансформ считается эффектом по прогрессу t ∈ [0, 1). В отличие от [OccasionalAnimator]
 * здесь нет пауз — объект «живёт» всё время.
 *
 * Чистая логика без GL, детерминирована; время ведёт вызывающая сторона (`update(delta)`).
 */
class ContinuousAnimator(
    private val period: Float,
    private val phase: Float,
    private val effect: (Float) -> ActTransform
) : IdleAnimator {

    private var time = 0f

    init {
        require(period > 0f) { "period должен быть > 0" }
    }

    override fun update(delta: Float) {
        if (delta <= 0f) return
        time = (time + delta) % period
    }

    override fun transform(): ActTransform {
        var t = (time + phase) % period
        if (t < 0f) t += period
        return effect(t / period)
    }
}
