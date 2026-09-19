package com.smitnk.motioncanvas.export

data class SpriteSheetLayout(val columns:Int,val rows:Int,val frameWidth:Int,val frameHeight:Int)
object SpriteSheetPlanner {
    fun plan(count:Int,w:Int,h:Int,maxWidth:Int=4096):SpriteSheetLayout{
        val cols=(maxWidth/w.coerceAtLeast(1)).coerceAtLeast(1).coerceAtMost(count.coerceAtLeast(1))
        return SpriteSheetLayout(cols,(count+cols-1)/cols,w,h)
    }
}