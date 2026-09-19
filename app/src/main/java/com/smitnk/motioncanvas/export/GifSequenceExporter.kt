package com.smitnk.motioncanvas.export

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/** Minimal animated GIF writer for editor previews. Uses indexed 256-color frames. */
object GifSequenceExporter {
    fun export(frames: List<Bitmap>, output: File, delayMs: Int) {
        require(frames.isNotEmpty())
        FileOutputStream(output).use { out ->
            out.write("GIF89a".toByteArray())
            val w=frames.first().width; val h=frames.first().height
            writeShort(out,w); writeShort(out,h); out.write(0xF7); out.write(0); out.write(0)
            for(i in 0 until 256){ out.write(i); out.write(i); out.write(i) }
            out.write(byteArrayOf(0x21,0xFF.toByte(),0x0B)); out.write("NETSCAPE2.0".toByteArray()); out.write(3);out.write(1);writeShort(out,0);out.write(0)
            frames.forEach { bitmap -> writeFrame(out,bitmap,delayMs/10) }
            out.write(0x3B)
        }
    }
    private fun writeFrame(out: FileOutputStream,b: Bitmap,delay:Int){
        out.write(byteArrayOf(0x21,0xF9.toByte(),4,0));writeShort(out,delay.coerceAtLeast(1));out.write(0);out.write(0)
        out.write(0x2C);writeShort(out,0);writeShort(out,0);writeShort(out,b.width);writeShort(out,b.height);out.write(0)
        val data=ByteArray(b.width*b.height); for(y in 0 until b.height)for(x in 0 until b.width){val c=b.getPixel(x,y); val g=(0.299*((c shr 16)and 255)+0.587*((c shr 8)and 255)+0.114*(c and 255)).toInt().coerceIn(0,255);data[y*b.width+x]=g.toByte()}
        out.write(8); val packed=lzw(data); var pos=0; while(pos<packed.size){val n=minOf(255,packed.size-pos);out.write(n);out.write(packed,pos,n);pos+=n};out.write(0)
    }
    private fun lzw(data:ByteArray):ByteArray{ val clear=256;val end=257;var codeSize=9;var next=258;var dict=HashMap<List<Int>,Int>();val out=ArrayList<Int>();out.add(clear);var prefix=mutableListOf<Int>();fun emit(c:Int){out.add(c);if(next<4096){next++;if(next==(1 shl codeSize)&&codeSize<12)codeSize++}}
        for(b in data){val k=b.toInt()and 255;val test=prefix+k;if(prefix.isNotEmpty()&&dict.containsKey(test)){prefix=test.toMutableList()}else{if(prefix.isNotEmpty())emit(if(prefix.size==1)prefix[0] else dict[prefix]!!);dict[test]=next;prefix=mutableListOf(k);}}
        if(prefix.isNotEmpty())emit(if(prefix.size==1)prefix[0] else dict[prefix]!!);emit(end);val bytes=ArrayList<Byte>();var bit=0;var cur=0;codeSize=9;next=258;for(c in out){cur=cur or (c shl bit);bit+=codeSize;while(bit>=8){bytes.add((cur and 255).toByte());cur=cur ushr 8;bit-=8};if(c==clear){codeSize=9;next=258}else if(c!=end&&next<4096){next++;if(next==(1 shl codeSize)&&codeSize<12)codeSize++}};if(bit>0)bytes.add(cur.toByte());return bytes.toByteArray() }
    private fun writeShort(out:FileOutputStream,v:Int){out.write(v and 255);out.write((v shr 8)and 255)}
}