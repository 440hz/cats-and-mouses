package com.catsandmouses.graphics

/**
 * Общий контракт аниматора объекта на карте: тик времени и текущий трансформ отрисовки.
 * Реализации — [OccasionalAnimator] (редкие акценты) и [ContinuousAnimator] (постоянный цикл).
 * Чистая логика без GL: время ведёт вызывающая сторона, поэтому анимацию можно замораживать.
 */
interface IdleAnimator {
    fun update(delta: Float)
    fun transform(): ActTransform
}
