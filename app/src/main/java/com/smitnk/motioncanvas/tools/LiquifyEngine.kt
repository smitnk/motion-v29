package com.smitnk.motioncanvas.tools

import android.graphics.Bitmap
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object LiquifyEngine {
    fun push(bitmap:Bitmap,cx:Float,cy:Float,dx:Float,dy:Float,radius:Float,strength:Float){
        if(bitmap.isRecycled)return
        val r=radius.coerceIn(2f,512f)
        val left=max(0,(cx-r).toInt()); val top=max(0,(cy-r).toInt())
        val right=min(bitmap.width,(cx+r).toInt()+1); val bottom=min(bitmap.height,(cy+r).toInt()+1)
        val src=bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888,true)
        for(y in top until bottom) for(x in left until right){
            val d=hypot(x-cx,y-cy)
            if(d>r)continue
            val fall=(1f-d/r).coerceIn(0f,1f)*strength.coerceIn(0f,1f)
            val sx=(x-dx*fall).coerceIn(0f,(bitmap.width-1).toFloat()).toInt()
            val sy=(y-dy*fall).coerceIn(0f,(bitmap.height-1).toFloat()).toInt()
            bitmap.setPixel(x,y,src.getPixel(sx,sy))
        }
        src.recycle()
    }
}