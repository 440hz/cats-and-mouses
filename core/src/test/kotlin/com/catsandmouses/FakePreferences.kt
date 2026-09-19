package com.catsandmouses

import com.badlogic.gdx.Preferences

/**
 * In-memory реализация [Preferences] для тестов: без LibGDX-бэкенда и файлов.
 * Позволяет проверить значения и число вызовов [flush].
 */
class FakePreferences : Preferences {

    private val values = mutableMapOf<String, Any>()

    var flushCount = 0
        private set

    override fun putBoolean(key: String, value: Boolean): Preferences {
        values[key] = value
        return this
    }

    override fun putInteger(key: String, value: Int): Preferences {
        values[key] = value
        return this
    }

    override fun putLong(key: String, value: Long): Preferences {
        values[key] = value
        return this
    }

    override fun putFloat(key: String, value: Float): Preferences {
        values[key] = value
        return this
    }

    override fun putString(key: String, value: String): Preferences {
        values[key] = value
        return this
    }

    override fun put(vals: MutableMap<String, *>): Preferences {
        vals.forEach { (key, value) -> if (value != null) values[key] = value }
        return this
    }

    override fun getBoolean(key: String): Boolean = getBoolean(key, false)

    override fun getInteger(key: String): Int = getInteger(key, 0)

    override fun getLong(key: String): Long = getLong(key, 0L)

    override fun getFloat(key: String): Float = getFloat(key, 0f)

    override fun getString(key: String): String? = getString(key, null)

    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

    override fun getInteger(key: String, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue

    override fun get(): MutableMap<String, *> = values.toMutableMap()

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun clear() {
        values.clear()
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun flush() {
        flushCount++
    }

    /** Снимок значений (для проверок). */
    fun snapshot(): Map<String, Any> = values.toMap()
}
