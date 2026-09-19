package com.smitnk.motioncanvas.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AColor
import androidx.compose.ui.graphics.toArgb
import com.smitnk.motioncanvas.Frame
import java.io.File
import java.io.FileOutputStream

object PngSequenceExporter {
    fun export(frames: List<Frame>, width: Int, height: Int, dir: File, background: Int = AColor.WHITE): List<File> {
        dir.mkdirs()
        return frames.mapIndexed { index, frame ->
            val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888); val canvas=Canvas(bitmap); canvas.drawColor(background)
            frame.strokes.forEach { stroke ->
                if(stroke.points.isEmpty()) return@forEach
                val path=Path(); path.moveTo(stroke.points.first().x,stroke.points.first().y)
                stroke.points.drop(1).forEach { path.lineTo(it.x,it.y) }
                val p=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=stroke.color.toArgb(); alpha=(stroke.alpha*255).toInt(); style=Paint.Style.STROKE; strokeWidth=stroke.strokeWidth; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND }
                canvas.drawPath(path,p)
            }
            File(dir,"frame_%05d.png".format(index)).also { FileOutputStream(it).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG,100,out) }; bitmap.recycle() }
        }
    }
}