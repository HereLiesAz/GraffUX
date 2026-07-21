// FILE: feature/editor/src/main/java/com/hereliesaz/graffitixr/feature/editor/SnapEngine.kt
package com.hereliesaz.graffitixr.feature.editor

import kotlin.math.abs

/**
 * The result of a snap: the [dx]/[dy] the moving box should shift so its nearest edge/centre lands on
 * a guide, plus the guide lines that became active ([guidesX] are vertical guides at those x's,
 * [guidesY] horizontal guides at those y's) so the UI can draw them. A no-op snap has zero deltas and
 * empty guides.
 */
class SnapResult(
    val dx: Float,
    val dy: Float,
    val guidesX: List<Float>,
    val guidesY: List<Float>,
) {
    val snapped: Boolean get() = guidesX.isNotEmpty() || guidesY.isNotEmpty()
}

/**
 * Pure snapping geometry — Figma/Procreate-style alignment. Given a moving box and a set of candidate
 * guide positions (typically the artboard's and other layers' left/centre/right and top/middle/bottom
 * lines), it nudges the box so its closest edge or centre aligns, within a pixel [threshold], on each
 * axis independently. Free of Android/Compose types so it's fully unit-testable; the caller supplies
 * boxes in whatever consistent space it snaps in (screen or world).
 */
object SnapEngine {

    /** Boxes are `[left, top, right, bottom]`. Returns the three x's (left, centreX, right). */
    private fun xEdges(box: FloatArray) = floatArrayOf(box[0], (box[0] + box[2]) / 2f, box[2])
    private fun yEdges(box: FloatArray) = floatArrayOf(box[1], (box[1] + box[3]) / 2f, box[3])

    /**
     * Snaps [box] (`[left, top, right, bottom]`) against vertical guides [targetsX] and horizontal
     * guides [targetsY], each within [threshold] pixels. Axes are independent.
     */
    fun snap(box: FloatArray, targetsX: List<Float>, targetsY: List<Float>, threshold: Float): SnapResult {
        val (dx, gx) = bestAxis(xEdges(box), targetsX, threshold)
        val (dy, gy) = bestAxis(yEdges(box), targetsY, threshold)
        return SnapResult(dx, dy, gx, gy)
    }

    /**
     * The candidate guide lines from an [artboard] box and the other layer [boxes]: each box
     * contributes its left/centre/right (x) and top/middle/bottom (y). Returns `(xLines, yLines)`.
     */
    fun guidesFrom(artboard: FloatArray, boxes: List<FloatArray>): Pair<List<Float>, List<Float>> {
        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        fun add(box: FloatArray) { xs.addAll(xEdges(box).toList()); ys.addAll(yEdges(box).toList()) }
        add(artboard)
        boxes.forEach(::add)
        return xs.distinct() to ys.distinct()
    }

    /** Finds the single best snap delta for [edges] against [targets], then the guide lines the
     *  shifted edges land on. */
    private fun bestAxis(edges: FloatArray, targets: List<Float>, threshold: Float): Pair<Float, List<Float>> {
        var bestDist = threshold
        var bestDelta = 0f
        var snapped = false
        for (e in edges) {
            for (t in targets) {
                val d = abs(t - e)
                if (d <= bestDist) {
                    bestDist = d
                    bestDelta = t - e
                    snapped = true
                }
            }
        }
        if (!snapped) return 0f to emptyList()
        val guides = targets.filter { t -> edges.any { e -> abs((e + bestDelta) - t) < 0.5f } }.distinct()
        return bestDelta to guides
    }
}
