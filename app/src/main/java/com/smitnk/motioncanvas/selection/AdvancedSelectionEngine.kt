package com.smitnk.motioncanvas.selection

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class RectSelection(val left:Float,val top:Float,val right:Float,val bottom:Float){
    fun contains(p:Offset)=p.x in left..right && p.y in top..bottom
}
object AdvancedSelectionEngine {
    fun polygonContains(poly:List<Offset>, p:Offset):Boolean {
        if(poly.size<3)return false
        var inside=false
        var j=poly.lastIndex
        for(i in poly.indices){
            val a=poly[i]; val b=poly[j]
            if((a.y>p.y)!=(b.y>p.y) &&
                p.x < (b.x-a.x)*(p.y-a.y)/(b.y-a.y).let{if(abs(it)<1e-6f) 1e-6f else it}+a.x) inside=!inside
            j=i
        }
        return inside
    }
    fun bounds(points:List<Offset>):RectSelection?{
        if(points.isEmpty())return null
        var l=points[0].x;var r=l;var t=points[0].y;var b=t
        for(p in points){l=min(l,p.x);r=max(r,p.x);t=min(t,p.y);b=max(b,p.y)}
        return RectSelection(l,t,r,b)
    }
}