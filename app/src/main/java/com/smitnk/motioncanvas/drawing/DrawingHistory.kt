/*
 * Drawing undo/redo architecture based on SmartToolFactory/Compose-Drawing-App
 * Reference: https://github.com/SmartToolFactory/Compose-Drawing-App
 *
 * MIT License
 * Copyright (c) 2022 SmartToolFactory
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.smitnk.motioncanvas.drawing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.smitnk.motioncanvas.DrawStroke
import com.smitnk.motioncanvas.Frame

sealed interface HistoryAction {
    data class AddSingleStroke(val stroke: DrawStroke) : HistoryAction
    data class AddMultipleStrokes(val addedStrokes: List<DrawStroke>) : HistoryAction
    data class DeleteMultipleStrokes(val deletedStrokes: List<Pair<Int, DrawStroke>>) : HistoryAction
    data class ReplaceMultipleStrokes(val before: List<DrawStroke>, val after: List<DrawStroke>) : HistoryAction
}

/**
 * Manages vector stroke drawing history, undo, and redo for an individual animation frame.
 *
 * Based on the path-action history model from SmartToolFactory/Compose-Drawing-App:
 * - Each stroke is stored as a vector object with points, color, stroke width, alpha, and eraser state.
 * - Supports drawing strokes as well as multi-stroke selection transformations (move, scale, rotate, flip, delete, duplicate, paste).
 * - Undo/Redo is animation-compatible and scoped strictly per [Frame], guaranteeing that
 *   undoing actions on Frame 2 never alters or clobbers Frame 1.
 * - High performance: Avoids heavy canvas bitmap caches by preserving vector paths.
 */
class FrameDrawingHistory(
    val frame: Frame,
    private val maxUndoHistory: Int = 100
) {
    // Observable snapshot lists consumed by Jetpack Compose Canvas
    val strokes: SnapshotStateList<DrawStroke> = mutableStateListOf<DrawStroke>().apply {
        addAll(frame.strokes)
    }
    val redoStrokes: SnapshotStateList<DrawStroke> = mutableStateListOf<DrawStroke>().apply {
        addAll(frame.redoStrokes)
    }

    private val undoStack = mutableListOf<HistoryAction>()
    private val redoStack = mutableListOf<HistoryAction>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty() || strokes.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty() || redoStrokes.isNotEmpty()

    /**
     * Records a new stroke (brush stroke or eraser stroke).
     * Automatically invalidates the redo history for this frame.
     */
    fun addStroke(stroke: DrawStroke) {
        if (strokes.size >= maxUndoHistory) {
            strokes.removeAt(0)
        }
        strokes.add(stroke)
        undoStack.add(HistoryAction.AddSingleStroke(stroke))
        redoStack.clear()
        redoStrokes.clear()
        syncWithFrame()
    }

    /**
     * Adds multiple strokes at once (e.g. paste or duplicate).
     */
    fun addStrokes(newStrokes: List<DrawStroke>) {
        if (newStrokes.isEmpty()) return
        strokes.addAll(newStrokes)
        undoStack.add(HistoryAction.AddMultipleStrokes(newStrokes))
        redoStack.clear()
        redoStrokes.clear()
        syncWithFrame()
    }

    /**
     * Deletes multiple strokes (e.g. delete selected artwork).
     */
    fun deleteStrokes(toDelete: List<DrawStroke>) {
        if (toDelete.isEmpty()) return
        val toDeleteIds = toDelete.map { it.id }.toSet()
        val indexedDeleted = mutableListOf<Pair<Int, DrawStroke>>()
        for (i in strokes.indices) {
            if (strokes[i].id in toDeleteIds) {
                indexedDeleted.add(Pair(i, strokes[i]))
            }
        }
        strokes.removeAll { it.id in toDeleteIds }
        undoStack.add(HistoryAction.DeleteMultipleStrokes(indexedDeleted))
        redoStack.clear()
        redoStrokes.clear()
        syncWithFrame()
    }

    /**
     * Replaces transformed strokes with their updated points (e.g. move, scale, rotate, flip).
     */
    fun replaceStrokes(before: List<DrawStroke>, after: List<DrawStroke>) {
        if (before.isEmpty() || after.isEmpty()) return
        val afterMap = after.associateBy { it.id }
        for (i in strokes.indices) {
            val updated = afterMap[strokes[i].id]
            if (updated != null) {
                strokes[i] = updated
            }
        }
        undoStack.add(HistoryAction.ReplaceMultipleStrokes(before, after))
        redoStack.clear()
        redoStrokes.clear()
        syncWithFrame()
    }

    /**
     * Undoes the most recent action on the active frame.
     */
    fun undo(): Boolean {
        if (undoStack.isNotEmpty()) {
            val action = undoStack.removeAt(undoStack.lastIndex)
            when (action) {
                is HistoryAction.AddSingleStroke -> {
                    strokes.remove(action.stroke)
                }
                is HistoryAction.AddMultipleStrokes -> {
                    val ids = action.addedStrokes.map { it.id }.toSet()
                    strokes.removeAll { it.id in ids }
                }
                is HistoryAction.DeleteMultipleStrokes -> {
                    val sorted = action.deletedStrokes.sortedBy { it.first }
                    for ((idx, stroke) in sorted) {
                        val safeIdx = idx.coerceIn(0, strokes.size)
                        strokes.add(safeIdx, stroke)
                    }
                }
                is HistoryAction.ReplaceMultipleStrokes -> {
                    val beforeMap = action.before.associateBy { it.id }
                    for (i in strokes.indices) {
                        val original = beforeMap[strokes[i].id]
                        if (original != null) {
                            strokes[i] = original
                        }
                    }
                }
            }
            redoStack.add(action)
            syncWithFrame()
            return true
        } else if (strokes.isNotEmpty()) {
            val lastStroke = strokes.removeAt(strokes.lastIndex)
            redoStrokes.add(lastStroke)
            syncWithFrame()
            return true
        }
        return false
    }

    /**
     * Redoes the most recently undone action on the active frame.
     */
    fun redo(): Boolean {
        if (redoStack.isNotEmpty()) {
            val action = redoStack.removeAt(redoStack.lastIndex)
            when (action) {
                is HistoryAction.AddSingleStroke -> {
                    strokes.add(action.stroke)
                }
                is HistoryAction.AddMultipleStrokes -> {
                    strokes.addAll(action.addedStrokes)
                }
                is HistoryAction.DeleteMultipleStrokes -> {
                    val ids = action.deletedStrokes.map { it.second.id }.toSet()
                    strokes.removeAll { it.id in ids }
                }
                is HistoryAction.ReplaceMultipleStrokes -> {
                    val afterMap = action.after.associateBy { it.id }
                    for (i in strokes.indices) {
                        val modified = afterMap[strokes[i].id]
                        if (modified != null) {
                            strokes[i] = modified
                        }
                    }
                }
            }
            undoStack.add(action)
            syncWithFrame()
            return true
        } else if (redoStrokes.isNotEmpty()) {
            val restoredStroke = redoStrokes.removeAt(redoStrokes.lastIndex)
            strokes.add(restoredStroke)
            syncWithFrame()
            return true
        }
        return false
    }

    /**
     * Clears all strokes on this frame.
     */
    fun clear() {
        strokes.clear()
        redoStrokes.clear()
        undoStack.clear()
        redoStack.clear()
        syncWithFrame()
    }

    private fun syncWithFrame() {
        frame.strokes.clear()
        frame.strokes.addAll(strokes)
        frame.redoStrokes.clear()
        frame.redoStrokes.addAll(redoStrokes)
    }
}

/**
 * Remember helper to instantiate and preserve [FrameDrawingHistory] scoped to the active frame ID.
 */
@Composable
fun rememberFrameDrawingHistory(frame: Frame): FrameDrawingHistory {
    return remember(frame.id) {
        FrameDrawingHistory(frame)
    }
}