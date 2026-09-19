package com.smitnk.motioncanvas.tools
import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.round
data class RulerResult(val start:Offset,val end:Offset,val angleDegrees:Float,val length:Float)
object PrecisionRulerEngine{
 fun measure(a:Offset,b:Offset):RulerResult{
  val dx=b.x-a.x;val dy=b.y-a.y
  return RulerResult(a,b,Math.toDegrees(atan2(dy.toDouble(),dx.toDouble())).toFloat(),kotlin.math.hypot(dx,dy))
 }
 fun snapAngle(a:Offset,b:Offset,step:Float=15f):Offset{
  val dx=b.x-a.x;val dy=b.y-a.y
  val len=kotlin.math.hypot(dx,dy)
  val angle=atan2(dy,dx)
  val snapped=round(Math.toDegrees(angle.toDouble()).toFloat()/step)*step
  val r=Math.toRadians(snapped.toDouble())
  return Offset(a.x+len*kotlin.math.cos(r).toFloat(),a.y+len*kotlin.math.sin(r).toFloat())
 }
}