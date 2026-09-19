package com.smitnk.motioncanvas.animation
import com.smitnk.motioncanvas.Stroke
data class FrameLayerData(val frame:Int,val layerIndex:Int,val strokes:List<Stroke>)
object PerFrameLayerEditor{
 fun replace(data:List<FrameLayerData>,frame:Int,layer:Int,strokes:List<Stroke>):List<FrameLayerData>{
  val out=data.toMutableList();val i=out.indexOfFirst{it.frame==frame&&it.layerIndex==layer}
  if(i>=0)out[i]=FrameLayerData(frame,layer,strokes) else out+=FrameLayerData(frame,layer,strokes)
  return out
 }
 fun strokes(data:List<FrameLayerData>,frame:Int,layer:Int)=data.firstOrNull{it.frame==frame&&it.layerIndex==layer}?.strokes.orEmpty()
}