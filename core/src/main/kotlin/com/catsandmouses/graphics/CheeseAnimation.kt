package com.catsandmouses.graphics

import com.catsandmouses.Constants

/**
 * Набор актов «изредка-анимации» сыра: покачивание, разбухание, сдувание.
 * Каждый акт короткий и возвращает сыр к стандартному виду; выбирается случайно.
 */
object CheeseAnimation {

    /** Покачивание: наклон на ±угол° (колебание влево-вправо). */
    val wobble = Act(Constants.CHEESE_WOBBLE_DURATION) { t ->
        ActTransform(rotation = Constants.CHEESE_WOBBLE_ANGLE * ActCurves.oscillation(t))
    }

    /** Разбухание: масштаб от 1.0 вверх до пика и обратно. */
    val swell = Act(Constants.CHEESE_SWELL_DURATION) { t ->
        ActTransform(scale = 1f + (Constants.CHEESE_SWELL_SCALE - 1f) * ActCurves.bell(t))
    }

    /** Сдувание: масштаб от 1.0 вниз (ниже стандарта) и обратно. */
    val deflate = Act(Constants.CHEESE_DEFLATE_DURATION) { t ->
        ActTransform(scale = 1f + (Constants.CHEESE_DEFLATE_SCALE - 1f) * ActCurves.bell(t))
    }

    val acts: List<Act> = listOf(wobble, swell, deflate)

    fun create(seed: Long): OccasionalAnimator = OccasionalAnimator(
        acts = acts,
        intervalMin = Constants.CHEESE_ANIM_INTERVAL_MIN,
        intervalMax = Constants.CHEESE_ANIM_INTERVAL_MAX,
        seed = seed
    )

    /** Стабильный seed по клетке — у каждого сыра своё детерминированное расписание. */
    fun cellSeed(col: Int, row: Int): Long = col * 9277L + row * 7043L
}
