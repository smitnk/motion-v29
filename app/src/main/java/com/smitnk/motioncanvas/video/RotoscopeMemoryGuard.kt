package com.smitnk.motioncanvas.video
data class RotoscopeMemoryPlan(val acceptedFrames:Int,val scale:Float,val estimatedBytes:Long)
object RotoscopeMemoryGuard{
 fun plan(sourceW:Int,sourceH:Int,requested:Int,maxBytes:Long=96L*1024*1024):RotoscopeMemoryPlan{
  var scale=1f
  var count=requested.coerceAtLeast(1)
  var bytes=sourceW.toLong()*sourceH*4L*count
  while(bytes>maxBytes&&scale>.2f){scale*=.85f;bytes=(sourceW*scale).toLong()*(sourceH*scale).toLong()*4L*count}
  while(bytes>maxBytes&&count>1){count=(count*.8f).toInt().coerceAtLeast(1);bytes=(sourceW*scale).toLong()*(sourceH*scale).toLong()*4L*count}
  return RotoscopeMemoryPlan(count,scale,bytes)
 }
}