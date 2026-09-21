package com.catsandmouses.graphics

import com.catsandmouses.Constants
import kotlin.random.Random

/**
 * Постоянный пульс сердечка на карте: плавное раздувание-сжатие по кругу, без пауз.
 * Трансформ — только масштаб: 1.0 → [Constants.HEART_PULSE_SCALE] → 1.0.
 */
object HeartAnimation {

    /** Трансформ по прогрессу цикла t ∈ [0, 1). */
    fun transformAt(t: Float): ActTransform =
        ActTransform(scale = 1f + (Constants.HEART_PULSE_SCALE - 1f) * ActCurves.pulse(t))

    /** Аниматор сердечка: фаза из seed разносит пульс, чтобы сердца не бились в один такт. */
    fun create(seed: Long): ContinuousAnimator = ContinuousAnimator(
        period = Constants.HEART_PULSE_PERIOD,
        phase = Random(seed).nextFloat() * Constants.HEART_PULSE_PERIOD,
        effect = ::transformAt
    )

    /** Стабильный seed по клетке — у каждого сердечка свой сдвиг фазы. */
    fun cellSeed(col: Int, row: Int): Long = col * 9283L + row * 6899L
}
