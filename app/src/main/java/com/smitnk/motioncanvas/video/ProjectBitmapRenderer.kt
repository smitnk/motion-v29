package com.smitnk.motioncanvas.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.smitnk.motioncanvas.Frame
import com.smitnk.motioncanvas.Project

/** Small deterministic renderer used by export; the editor remains the interactive renderer. */
object ProjectBitmapRenderer {
    fun render(project: Project, frame: Frame): Bitmap {
        val bitmap = Bitmap.createBitmap(project.canvasW, project.canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(project.backgroundColor.toArgb())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        frame.strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            paint.color = if (stroke.isEraser) project.backgroundColor.toArgb() else stroke.color.copy(alpha = stroke.alpha).toArgb()
            paint.strokeWidth = stroke.strokeWidth
            val path = Path().apply {
                moveTo(stroke.points.first().x, stroke.points.first().y)
                stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            canvas.drawPath(path, paint)
        }
        frame.texts.forEach { text ->
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = text.color.toArgb(); textSize = text.size; typeface = Typeface.DEFAULT
            }
            canvas.drawText(text.text, text.x, text.y + text.size, tp)
        }
        return bitmap
    }

    private fun androidx.compose.ui.graphics.Color.toArgb(): Int =
        android.graphics.Color.argb((alpha * 255f).toInt(), (red * 255f).toInt(), (green * 255f).toInt(), (blue * 255f).toInt())
}