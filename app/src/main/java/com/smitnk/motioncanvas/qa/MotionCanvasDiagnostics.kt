package com.smitnk.motioncanvas.qa

data class DiagnosticIssue(
    val severity: Severity,
    val component: String,
    val message: String
)

enum class Severity { INFO, WARNING, ERROR }

object MotionCanvasDiagnostics {
    fun checkTimeline(frameCount: Int, currentFrame: Int, fps: Int): List<DiagnosticIssue> =
        buildList {
            if (frameCount <= 0) add(DiagnosticIssue(Severity.ERROR, "Timeline", "Project has no frames."))
            if (currentFrame !in 0 until frameCount && frameCount > 0)
                add(DiagnosticIssue(Severity.ERROR, "Timeline", "Current frame is outside the frame range."))
            if (fps !in 1..60)
                add(DiagnosticIssue(Severity.WARNING, "Timeline", "FPS is outside the supported 1–60 range."))
        }

    fun checkAudio(startFrame: Int, endFrame: Int): List<DiagnosticIssue> =
        buildList {
            if (startFrame < 0) add(DiagnosticIssue(Severity.ERROR, "Audio", "Clip starts before frame 0."))
            if (endFrame <= startFrame) add(DiagnosticIssue(Severity.ERROR, "Audio", "Audio clip has an invalid duration."))
        }

    fun checkReference(scale: Float, opacity: Float): List<DiagnosticIssue> =
        buildList {
            if (scale <= 0f) add(DiagnosticIssue(Severity.ERROR, "Reference", "Reference scale must be positive."))
            if (opacity !in 0f..1f) add(DiagnosticIssue(Severity.WARNING, "Reference", "Reference opacity should be between 0 and 1."))
        }
}