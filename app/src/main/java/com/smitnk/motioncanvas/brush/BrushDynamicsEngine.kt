package com.smitnk.motioncanvas.brush
data class BrushDynamics(val pressureSize:Float=1f,val pressureOpacity:Float=1f,val spacing:Float=.18f,val smoothing:Float=.65f)
object BrushDynamicsEngine{
 fun size(base:Float,pressure:Float,d:BrushDynamics)=base*(1f+(pressure.coerceIn(0f,1f)-.5f)*d.pressureSize)
 fun opacity(base:Float,pressure:Float,d:BrushDynamics)=base*(1f+(pressure.coerceIn(0f,1f)-.5f)*d.pressureOpacity).coerceIn(0f,1f)
}