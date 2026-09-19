package com.smitnk.motioncanvas.animation

/**
 * Batch timeline operations. Independently implemented for MotionCanvas.
 * Feature concepts are common in open-source cel animation editors.
 */
object BatchFrameOperations {
    fun duplicateRange(
        frames: List<Int>,
        start: Int,
        endInclusive: Int,
        insertAt: Int
    ): List<Int> {
        if (frames.isEmpty()) return frames
        val a = start.coerceIn(0, frames.lastIndex)
        val b = endInclusive.coerceIn(a, frames.lastIndex)
        val block = frames.subList(a, b + 1)
        val out = frames.toMutableList()
        val pos = insertAt.coerceIn(0, out.size)
        out.addAll(pos, block)
        return out
    }

    fun deleteRange(frames: List<Int>, start: Int, endInclusive: Int): List<Int> {
        if (frames.isEmpty()) return frames
        val a = start.coerceIn(0, frames.lastIndex)
        val b = endInclusive.coerceIn(a, frames.lastIndex)
        return frames.filterIndexed { i, _ -> i !in a..b }
    }

    /**
     * Applies duplication to the real MotionCanvas frame list.
     * The copied frames receive new IDs so they are independent timeline frames.
     */
    fun duplicate(frames: MutableList<com.smitnk.motioncanvas.Frame>, selected: Set<Int>) {
        val indices = selected.filter { it in frames.indices }.sorted()
        if (indices.isEmpty()) return
        val copies = indices.map { frames[it].deepCopyForTimeline() }
        val insertAt = (indices.last() + 1).coerceAtMost(frames.size)
        frames.addAll(insertAt, copies)
    }

    fun delete(frames: MutableList<com.smitnk.motioncanvas.Frame>, selected: Set<Int>) {
        selected.filter { it in frames.indices }.sortedDescending().forEach { frames.removeAt(it) }
        if (frames.isEmpty()) frames.add(com.smitnk.motioncanvas.Frame())
    }

    fun move(frames: MutableList<com.smitnk.motioncanvas.Frame>, start: Int, endInclusive: Int, target: Int) {
        if (frames.isEmpty()) return
        val a = start.coerceIn(0, frames.lastIndex)
        val b = endInclusive.coerceIn(a, frames.lastIndex)
        val block = frames.subList(a, b + 1).map { it.deepCopyForTimeline() }
        repeat(b - a + 1) { frames.removeAt(a) }
        val pos = target.coerceIn(0, frames.size)
        frames.addAll(pos, block)
    }

    fun moveRange(frames: List<Int>, start: Int, endInclusive: Int, target: Int): List<Int> {
        if (frames.isEmpty()) return frames
        val a = start.coerceIn(0, frames.lastIndex)
        val b = endInclusive.coerceIn(a, frames.lastIndex)
        val block = frames.subList(a, b + 1).toList()
        val remaining = frames.filterIndexed { i, _ -> i !in a..b }.toMutableList()
        val pos = target.coerceIn(0, remaining.size)
        remaining.addAll(pos, block)
        return remaining
    }
}


private fun com.smitnk.motioncanvas.Frame.deepCopyForTimeline(): com.smitnk.motioncanvas.Frame =
    copy(
        id = java.util.UUID.randomUUID().toString(),
        strokes = strokes.map { it.deepCopy() }.toMutableList(),
        texts = texts.toMutableList(),
        redoStrokes = mutableListOf(),
        fills = fills.toMutableList()
    )