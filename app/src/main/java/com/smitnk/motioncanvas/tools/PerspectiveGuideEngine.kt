package com.smitnk.motioncanvas.tools
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
data class PerspectiveGuide(val horizonY:Float, val vanishingPoints:List<Offset>, val visible:Boolean=true)
object PerspectiveGuideEngine{
 fun snap(point:Offset,guide:PerspectiveGuide):Offset{
  if(guide.vanishingPoints.isEmpty())return point
  val vp=guide.vanishingPoints.minBy{abs(it.x-point.x)+abs(it.y-point.y)}
  val t=((point.y-guide.horizonY)/(vp.y-guide.horizonY).let{if(abs(it)<0.001f)0.001f else it})
  return Offset(vp.x+(point.x-vp.x)*t,point.y)
 }
}