package com.smitnk.motioncanvas.drawing

import android.graphics.Bitmap

object SmudgeEngine {
    fun smudge(bitmap: Bitmap, cx:Int, cy:Int, radius:Int=24, strength:Float=.35f): Bitmap {
        val out=bitmap.copy(Bitmap.Config.ARGB_8888,true); val src=bitmap.copy(Bitmap.Config.ARGB_8888,false)
        val r=radius.coerceAtLeast(2); val s=strength.coerceIn(0f,1f)
        for(y in (cy-r).coerceAtLeast(0)..(cy+r).coerceAtMost(bitmap.height-1)) for(x in (cx-r).coerceAtLeast(0)..(cx+r).coerceAtMost(bitmap.width-1)) {
            val dx=x-cx; val dy=y-cy; if(dx*dx+dy*dy<=r*r){ val sx=(cx-dx*.35f).toInt().coerceIn(0,bitmap.width-1); val sy=(cy-dy*.35f).toInt().coerceIn(0,bitmap.height-1); val a=src.getPixel(x,y); val b=src.getPixel(sx,sy); val c=android.graphics.Color.argb(android.graphics.Color.alpha(a), (android.graphics.Color.red(a)*(1-s)+android.graphics.Color.red(b)*s).toInt(), (android.graphics.Color.green(a)*(1-s)+android.graphics.Color.green(b)*s).toInt(), (android.graphics.Color.blue(a)*(1-s)+android.graphics.Color.blue(b)*s).toInt()); out.setPixel(x,y,c) }
        }
        return out
    }
}