package com.smitnk.motioncanvas.rigging

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Bone(val start:Offset,val length:Float,val angle:Float)
object IKChainEngine {
    fun solve(chain:List<Bone>,target:Offset,iterations:Int=8):List<Bone>{
        if(chain.isEmpty())return chain
        val out=chain.toMutableList()
        repeat(iterations.coerceIn(1,32)){
            var end=out.fold(out.first().start){p,b->Offset(p.x+cos(b.angle)*b.length,p.y+sin(b.angle)*b.length)}
            for(i in out.indices.reversed()){
                val b=out[i]
                val pivot=b.start
                val a0=atan2(end.y-pivot.y,end.x-pivot.x)
                val a1=atan2(target.y-pivot.y,target.x-pivot.x)
                val delta=a1-a0
                out[i]=b.copy(angle=b.angle+delta)
                end=out.fold(out.first().start){p,q->Offset(p.x+cos(q.angle)*q.length,p.y+sin(q.angle)*q.length)}
            }
        }
        return out
    }
}