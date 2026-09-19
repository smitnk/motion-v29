package com.smitnk.motioncanvas.project
import android.content.Context
import java.io.File
class AutosaveStore(context: Context) {
    private val dir = File(context.filesDir, "autosaves").apply { mkdirs() }
    fun slot(projectId: String): File = File(dir, "$projectId.autosave.json")
    fun save(projectId: String, json: String) { slot(projectId).writeText(json) }
    fun load(projectId: String): String? = slot(projectId).takeIf { it.exists() }?.readText()
    fun clear(projectId: String) { slot(projectId).delete() }
}