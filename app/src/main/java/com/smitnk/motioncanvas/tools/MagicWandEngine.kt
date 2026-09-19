package com.smitnk.motioncanvas.tools

import android.graphics.Bitmap
import java.util.ArrayDeque
import kotlin.math.abs

object MagicWandEngine {
    fun select(bitmap:Bitmap,x:Int,y:Int,tolerance:Int=24):BooleanArray{
        val w=bitmap.width; val h=bitmap.height
        val mask=BooleanArray(w*h)
        if(x !in 0 until w || y !in 0 until h)return mask
        val target=bitmap.getPixel(x,y)
        fun close(c:Int):Boolean{
            return abs(((c shr 16)and 255)-((target shr 16)and 255))<=tolerance &&
                   abs(((c shr 8)and 255)-((target shr 8)and 255))<=tolerance &&
                   abs((c and 255)-(target and 255))<=tolerance &&
                   abs(((c ushr 24)and 255)-((target ushr 24)and 255))<=tolerance
        }
        val q=ArrayDeque<Int>(); q.add(y*w+x); mask[y*w+x]=true
        while(q.isNotEmpty()){
            val p=q.removeFirst(); val px=p%w; val py=p/w
            val ns=intArrayOf(p-1,p+1,p-w,p+w)
            for(n in ns){
                if(n<0||n>=w*h)continue
                val nx=n%w; val ny=n/w
                if(abs(nx-px)+abs(ny-py)!=1 || mask[n] || !close(bitmap.getPixel(nx,ny)))continue
                mask[n]=true;q.add(n)
            }
        }
        return mask
    }
}