package com.smitnk.motioncanvas.project

import java.io.File

object AutosaveRecovery {
    fun write(file:File,data:String){
        val tmp=File(file.parentFile,file.name+".tmp")
        tmp.writeText(data)
        if(file.exists()) file.delete()
        tmp.renameTo(file)
    }
    fun readIfPresent(file:File):String?=if(file.exists())file.readText()else null
}