package com.hereliesaz.graffitixr.common.util

import androidx.compose.ui.geometry.Offset

/**
 * Implements brush stabilization algorithms to smooth out raw input points.
 */
class StrokeStabilizer {

    private val history = mutableListOf<Offset>()

    /**
     * Stabilizes a raw input point using a weighted moving average.
     * @param rawPoint The raw input point from the touch/pointer event.
     * @param level The stabilization level (0 = disabled, 1-100 = active).
     * @return The stabilized point to draw.
     */
    fun stabilize(rawPoint: Offset, level: Int): Offset {
        if (level <= 0) return rawPoint

        // History capacity scales with level, e.g., max 20 points
        val capacity = (level / 100f * 20).toInt().coerceAtLeast(1)

        history.add(rawPoint)
        if (history.size > capacity) {
            history.removeAt(0)
        }

        var sumX = 0f
        var sumY = 0f
        var totalWeight = 0f

        // Exponential weighting: recent points matter more
        for (i in history.indices) {
            val weight = (i + 1).toFloat()
            sumX += history[i].x * weight
            sumY += history[i].y * weight
            totalWeight += weight
        }

        return Offset(sumX / totalWeight, sumY / totalWeight)
    }

    /**
     * Resets the stabilizer history, called on onStrokeStart.
     */
    fun reset() {
        history.clear()
    }
}
