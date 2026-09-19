package com.smitnk.motioncanvas.project

import android.content.Context
import com.smitnk.motioncanvas.DrawPoint
import com.smitnk.motioncanvas.DrawStroke
import com.smitnk.motioncanvas.Frame
import com.smitnk.motioncanvas.Layer
import com.smitnk.motioncanvas.Project
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ProjectRepository {
    private const val EXT = ".motioncanvas.json"

    fun save(context: Context, project: Project): File {
        val file = File(context.filesDir, project.name.sanitize() + EXT)
        val temp = File(context.filesDir, file.name + ".tmp")
        temp.writeText(encode(project).toString(2))
        if (!temp.renameTo(file)) { file.writeText(temp.readText()); temp.delete() }
        return file
    }

    fun load(context: Context, name: String): Project? {
        val file = File(context.filesDir, name.sanitize() + EXT)
        return if (file.exists()) decode(JSONObject(file.readText())) else null
    }

    fun list(context: Context): List<String> = context.filesDir.listFiles()
        ?.filter { it.name.endsWith(EXT) }?.map { it.name.removeSuffix(EXT) }?.sorted() ?: emptyList()

    fun encode(p: Project): JSONObject = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("fps", p.fps)
        put("canvasW", p.canvasW); put("canvasH", p.canvasH)
        put("background", p.backgroundColor.toArgb())
        put("frames", JSONArray().apply { p.frames.forEach { put(frame(it)) } })
        put("layers", JSONArray().apply { p.layers.forEach { put(layer(it)) } })
        put("audioPath", p.audioPath ?: JSONObject.NULL)
        put("audioClips", JSONArray().apply { p.audioClips.forEach { put(audioClip(it)) } })
        put("audioTracks", JSONArray().apply { p.audioTracks.forEach { t -> put(JSONObject().apply { put("id",t.id); put("name",t.name); put("enabled",t.enabled); put("clips",JSONArray().apply { t.clips.forEach { put(audioClip(it)) } }) }) } })
    }

    private fun audioClip(c: com.smitnk.motioncanvas.audio.AudioClip) = JSONObject().apply { put("id",c.id); put("uri",c.uri); put("startFrame",c.startFrame); put("durationFrames",c.durationFrames); put("volume",c.volume); put("muted",c.muted); put("inFrame",c.inFrame); put("outFrame",c.outFrame); put("fadeInFrames",c.fadeInFrames); put("fadeOutFrames",c.fadeOutFrames) }

    private fun frame(f: Frame) = JSONObject().apply {
        put("id", f.id); put("strokes", JSONArray().apply { f.strokes.forEach { put(stroke(it)) } })
        put("durationFrames", f.durationFrames); put("keyframe", f.isKeyframe); put("tag", f.tag); put("tagColor", f.tagColor.toArgb())
        put("fills", JSONArray().apply { f.fills.forEach { put(JSONObject().apply { put("x", it.x); put("y", it.y); put("color", it.color.toArgb()); put("tolerance", it.tolerance) }) } })
        put("texts", JSONArray().apply { f.texts.forEach { put(JSONObject().apply { put("id", it.id); put("text", it.text); put("x", it.x); put("y", it.y); put("size", it.size); put("color", it.color.toArgb()) }) } })
    }
    private fun layer(l: Layer) = JSONObject().apply { put("id", l.id); put("name", l.name); put("visible", l.visible); put("opacity", l.opacity) }
    private fun stroke(s: DrawStroke) = JSONObject().apply {
        put("id", s.id); put("color", s.color.toArgb()); put("width", s.strokeWidth); put("alpha", s.alpha); put("eraser", s.isEraser)
        put("points", JSONArray().apply { s.points.forEach { put(JSONObject().apply { put("x", it.x); put("y", it.y); put("pressure", it.pressure) }) } })
    }

    private fun decode(o: JSONObject): Project {
        val frames = mutableListOf<Frame>(); val fs = o.optJSONArray("frames") ?: JSONArray()
        for (i in 0 until fs.length()) { val f = fs.getJSONObject(i); val strokes = mutableListOf<DrawStroke>(); val ss = f.optJSONArray("strokes") ?: JSONArray()
            for (j in 0 until ss.length()) { val s=ss.getJSONObject(j); val pts=mutableListOf<DrawPoint>(); val ps=s.optJSONArray("points") ?: JSONArray(); for(k in 0 until ps.length()){val q=ps.getJSONObject(k); pts.add(DrawPoint(q.getDouble("x").toFloat(),q.getDouble("y").toFloat(),q.optDouble("pressure",1.0).toFloat()))}; strokes.add(DrawStroke(s.getString("id"),pts,Color(s.optLong("color",Color.Black.toArgb().toLong()).toInt()),s.optDouble("width",8.0).toFloat(),s.optDouble("alpha",1.0).toFloat(),s.optBoolean("eraser",false))) }
            val fills = mutableListOf<com.smitnk.motioncanvas.FillMark>(); val ff=f.optJSONArray("fills") ?: JSONArray(); for(k in 0 until ff.length()){ val q=ff.getJSONObject(k); fills.add(com.smitnk.motioncanvas.FillMark(q.optInt("x"),q.optInt("y"),Color(q.optInt("color",Color.Black.toArgb())),q.optInt("tolerance",12))) }
            val texts = mutableListOf<com.smitnk.motioncanvas.CanvasText>(); val tt=f.optJSONArray("texts") ?: JSONArray(); for(k in 0 until tt.length()){ val q=tt.getJSONObject(k); texts.add(com.smitnk.motioncanvas.CanvasText(q.optString("id"),q.optString("text"),q.optDouble("x").toFloat(),q.optDouble("y").toFloat(),q.optDouble("size",40.0).toFloat(),Color(q.optInt("color",Color.Black.toArgb())))) }
            frames.add(Frame(f.getString("id"),strokes,texts, mutableListOf(), fills, f.optInt("durationFrames",1), f.optBoolean("keyframe",false), f.optString("tag",""), Color(f.optInt("tagColor",Color.Transparent.toArgb()))))
        }
        if(frames.isEmpty()) frames.add(Frame())
        val layers=mutableListOf<Layer>(); val ls=o.optJSONArray("layers") ?: JSONArray(); for(i in 0 until ls.length()){val l=ls.getJSONObject(i); layers.add(Layer(l.getString("id"),l.getString("name"),l.optBoolean("visible",true),l.optDouble("opacity",1.0).toFloat()))}; if(layers.isEmpty()) layers.add(Layer())
        val audio = if (o.isNull("audioPath")) null else o.optString("audioPath", null)
        val clips = mutableListOf<com.smitnk.motioncanvas.audio.AudioClip>(); val ac=o.optJSONArray("audioClips") ?: JSONArray(); for(i in 0 until ac.length()){ val q=ac.getJSONObject(i); clips.add(com.smitnk.motioncanvas.audio.AudioClip(q.optString("id"),q.optString("uri"),q.optInt("startFrame",0),q.optInt("durationFrames",1),q.optDouble("volume",1.0).toFloat(),q.optBoolean("muted",false),q.optInt("inFrame",0),q.optInt("outFrame",Int.MAX_VALUE),q.optInt("fadeInFrames",0),q.optInt("fadeOutFrames",0))) }
        val tracks = mutableListOf<com.smitnk.motioncanvas.audio.AudioTrack>(); val ats=o.optJSONArray("audioTracks") ?: JSONArray(); for(i in 0 until ats.length()){ val t=ats.getJSONObject(i); val tc=mutableListOf<com.smitnk.motioncanvas.audio.AudioClip>(); val ca=t.optJSONArray("clips") ?: JSONArray(); for(j in 0 until ca.length()){ val q=ca.getJSONObject(j); tc.add(com.smitnk.motioncanvas.audio.AudioClip(q.optString("id"),q.optString("uri"),q.optInt("startFrame",0),q.optInt("durationFrames",1),q.optDouble("volume",1.0).toFloat(),q.optBoolean("muted",false),q.optInt("inFrame",0),q.optInt("outFrame",Int.MAX_VALUE),q.optInt("fadeInFrames",0),q.optInt("fadeOutFrames",0))) }; tracks.add(com.smitnk.motioncanvas.audio.AudioTrack(t.optString("id","track-$i"),t.optString("name","Audio ${i+1}"),tc,t.optBoolean("enabled",true))) }
        if(tracks.isEmpty() && clips.isNotEmpty()) tracks.add(com.smitnk.motioncanvas.audio.AudioTrack("track-1","Audio 1",clips.toMutableList()))
        return Project(o.getString("id"),o.getString("name"),o.optInt("fps",12),o.optInt("canvasW",1280),o.optInt("canvasH",720),frames,layers,Color(o.optLong("background",Color.White.toArgb().toLong()).toInt()),audio,clips,tracks)
    }
    private fun String.sanitize() = replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "Untitled" }
}