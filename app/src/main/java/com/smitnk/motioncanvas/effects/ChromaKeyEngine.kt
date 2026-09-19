package com.smitnk.motioncanvas.effects

import android.graphics.Bitmap
import kotlin.math.abs

object ChromaKeyEngine {
    fun removeColor(source: Bitmap, targetRgb: Int, tolerance: Int = 60, softness: Int = 12): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val tr = (targetRgb shr 16) and 255; val tg = (targetRgb shr 8) and 255; val tb = targetRgb and 255
        val px = IntArray(out.width * out.height); out.getPixels(px, 0, out.width, 0, 0, out.width, out.height)
        val soft = softness.coerceAtLeast(1)
        for (i in px.indices) {
            val c = px[i]; val r=(c shr 16) and 255; val g=(c shr 8) and 255; val b=c and 255
            val d=maxOf(abs(r-tr), abs(g-tg), abs(b-tb))
            if (d <= tolerance) px[i] = c and 0x00FFFFFF else if (d <= tolerance + soft) {
                val a=((d-tolerance).toFloat()/soft*255f).toInt().coerceIn(0,255)
                px[i]=(a shl 24) or (c and 0x00FFFFFF)
            }
        }
        out.setPixels(px,0,out.width,0,0,out.width,out.height); return out
    }
}