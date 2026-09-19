package com.smitnk.motioncanvas.tools
import androidx.compose.ui.geometry.Offset
data class BezierPath(val p0:Offset,val p1:Offset,val p2:Offset,val p3:Offset)
object BezierPathEngine{
 fun sample(c:BezierPath,t0:Float):Offset{
  val t=t0.coerceIn(0f,1f);val u=1f-t
  return Offset(
   u*u*u*c.p0.x+3*u*u*t*c.p1.x+3*u*t*t*c.p2.x+t*t*t*c.p3.x,
   u*u*u*c.p0.y+3*u*u*t*c.p1.y+3*u*t*t*c.p2.y+t*t*t*c.p3.y)
 }
 fun flatten(c:BezierPath,segments:Int=32)=List(segments.coerceAtLeast(2)){i->sample(c,i.toFloat()/(segments-1))}
}