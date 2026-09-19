package com.smitnk.motioncanvas.fill
import kotlin.math.abs
object FloodFillEngine {
    fun fill(pixels: IntArray, width: Int, height: Int, x: Int, y: Int, replacement: Int, tolerance: Int = 8): Boolean {
        if (width <= 0 || height <= 0 || x !in 0 until width || y !in 0 until height) return false
        val start = pixels[y * width + x]; if (start == replacement) return false
        fun matches(c: Int): Boolean {
            val dr = abs(((c ushr 16) and 255) - ((start ushr 16) and 255))
            val dg = abs(((c ushr 8) and 255) - ((start ushr 8) and 255))
            val db = abs((c and 255) - (start and 255))
            return dr <= tolerance && dg <= tolerance && db <= tolerance
        }
        val stack = IntArray(width * height); var top = 0
        val seen = BooleanArray(width * height)
        stack[top++] = y * width + x
        while (top > 0) {
            val idx = stack[--top]; if (seen[idx]) continue; seen[idx] = true
            if (!matches(pixels[idx])) continue
            pixels[idx] = replacement
            val cx = idx % width; val cy = idx / width
            if (cx > 0) stack[top++] = idx - 1
            if (cx + 1 < width) stack[top++] = idx + 1
            if (cy > 0) stack[top++] = idx - width
            if (cy + 1 < height) stack[top++] = idx + width
        }
        return true
    }
}