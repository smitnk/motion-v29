package com.smitnk.motioncanvas.audio
data class WaveformHandleState(val inFrame:Int=0,val outFrame:Int=1,val zoom:Float=1f){
 fun trimIn(v:Int)=copy(inFrame=v.coerceAtLeast(0).coerceAtMost(outFrame-1))
 fun trimOut(v:Int)=copy(outFrame=v.coerceAtLeast(inFrame+1))
}