package com.smitnk.motioncanvas.brush

class BrushPresetStore {
    private val items = mutableListOf<CustomBrushPreset>()
    val presets: List<CustomBrushPreset> get() = items.toList()

    fun add(preset: CustomBrushPreset) { items.removeAll { it.name == preset.name }; items.add(preset) }
    fun remove(id: String) { items.removeAll { it.id == id } }
}