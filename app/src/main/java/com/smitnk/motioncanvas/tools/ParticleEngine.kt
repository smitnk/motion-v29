package com.smitnk.motioncanvas.tools

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(var x:Float,var y:Float,var vx:Float,var vy:Float,var life:Float,var size:Float)
data class ParticleEmitter(val x:Float,val y:Float,val rate:Int=12,val speed:Float=80f,val gravity:Float=120f)

object ParticleEngine {
    fun emit(e:ParticleEmitter,count:Int=e.rate):List<Particle> = List(count.coerceAtLeast(0)){
        val a=Random.nextDouble(0.0,Math.PI*2).toFloat()
        val s=e.speed*Random.nextFloat()
        Particle(e.x,e.y,cos(a)*s,sin(a)*s,1f,2f+Random.nextFloat()*6f)
    }
    fun step(p:Particle,dt:Float,g:Float=120f):Particle{
        p.x+=p.vx*dt;p.y+=p.vy*dt;p.vy+=g*dt;p.life-=dt
        return p
    }
}