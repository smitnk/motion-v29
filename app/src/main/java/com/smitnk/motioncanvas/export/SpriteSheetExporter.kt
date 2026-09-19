package com.smitnk.motioncanvas.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import androidx.compose.ui.graphics.toArgb
import com.smitnk.motioncanvas.Project
import java.io.File
import java.io.FileOutputStream

object SpriteSheetExporter {
    fun export(project: Project, output: File, columns: Int = 4): Result<File> = runCatching {
        require(project.frames.isNotEmpty())
        val cellW = project.canvasW.coerceAtLeast(1); val cellH = project.canvasH.coerceAtLeast(1)
        val cols = columns.coerceIn(1, 8); val rows = (project.frames.size + cols - 1) / cols
        val sheet = Bitmap.createBitmap(cellW * cols, cellH * rows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet); canvas.drawColor(project.backgroundColor.toArgb())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND; style=Paint.Style.STROKE }
        project.frames.forEachIndexed { index, frame ->
            val ox=(index%cols)*cellW; val oy=(index/cols)*cellH; canvas.save(); canvas.translate(ox.toFloat(),oy.toFloat())
            frame.strokes.forEach { s -> if(s.points.size>1){ paint.color=if(s.isEraser) project.backgroundColor.toArgb() else s.color.copy(alpha=s.alpha).toArgb(); paint.strokeWidth=s.strokeWidth; val p=Path(); p.moveTo(s.points[0].x,s.points[0].y); s.points.drop(1).forEach{p.lineTo(it.x,it.y)}; canvas.drawPath(p,paint) } }
            canvas.restore()
        }
        output.parentFile?.mkdirs(); FileOutputStream(output).use { sheet.compress(Bitmap.CompressFormat.PNG,100,it) }; sheet.recycle(); output
    }
}