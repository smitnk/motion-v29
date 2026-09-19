package com.smitnk.motioncanvas.animation
import com.smitnk.motioncanvas.Stroke
import com.smitnk.motioncanvas.selection.MultiFrameTransformEngine
import com.smitnk.motioncanvas.selection.MultiFrameTransform
object BatchTransformEngine{
 fun apply(frames:List<List<Stroke>>,transform:MultiFrameTransform,from:Int,to:Int):List<List<Stroke>>=
  frames.mapIndexed{i,f->if(i in from..to)MultiFrameTransformEngine.applyToFrames(listOf(f),transform).first() else f}
}