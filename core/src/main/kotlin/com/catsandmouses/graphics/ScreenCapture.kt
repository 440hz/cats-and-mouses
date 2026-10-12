package com.catsandmouses.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import java.io.File

/**
 * Dev-утилита: сохранить текущий кадр в PNG (`--shot`, только Desktop).
 * Читает кадровый буфер (glReadPixels — снизу вверх) и переворачивает по вертикали.
 */
object ScreenCapture {

    fun save(path: String) {
        val w = Gdx.graphics.backBufferWidth
        val h = Gdx.graphics.backBufferHeight
        val src = Pixmap.createFromFrameBuffer(0, 0, w, h)
        val dst = Pixmap(w, h, Pixmap.Format.RGBA8888)
        flipVertically(src, dst, w, h)
        PixmapIO.writePNG(Gdx.files.absolute(File(path).absolutePath), dst)
        dst.dispose()
        src.dispose()
    }

    private fun flipVertically(src: Pixmap, dst: Pixmap, w: Int, h: Int) {
        val rowBytes = w * 4
        val s = src.pixels
        val d = dst.pixels
        for (row in 0 until h) {
            val srcPos = row * rowBytes
            val dstPos = (h - 1 - row) * rowBytes
            for (i in 0 until rowBytes) d.put(dstPos + i, s.get(srcPos + i))
        }
    }
}
