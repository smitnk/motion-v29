package com.smitnk.motioncanvas.camera

data class Camera2D(val x:Float=0f,val y:Float=0f,val zoom:Float=1f,val rotation:Float=0f){
    fun moved(dx:Float,dy:Float)=copy(x=x+dx,y=y+dy)
    fun zoomed(f:Float)=copy(zoom=(zoom*f).coerceIn(.05f,50f))
}