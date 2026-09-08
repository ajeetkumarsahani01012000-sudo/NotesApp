package com.example.noteapp.data

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float
)

data class Stroke(
    val points: MutableList<StrokePoint> = mutableListOf(),
    var color: Int = 0xFF000000.toInt(),
    var baseWidth: Float = 6f,
    var toolType: ToolType = ToolType.PEN
)

enum class ToolType {
    PEN,
    HIGHLIGHTER,
    ERASER
}
