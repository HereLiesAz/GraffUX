// FILE: feature/editor/src/test/java/com/hereliesaz/graffitixr/feature/editor/SnapEngineTest.kt
package com.hereliesaz.graffitixr.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapEngineTest {

    private fun box(l: Float, t: Float, r: Float, b: Float) = floatArrayOf(l, t, r, b)

    @Test
    fun snapsLeftEdgeToGuideWithinThreshold() {
        val r = SnapEngine.snap(box(100f, 0f, 200f, 100f), targetsX = listOf(105f), targetsY = emptyList(), threshold = 10f)
        assertEquals(5f, r.dx, 1e-3f) // left 100 → 105
        assertEquals(0f, r.dy, 1e-3f)
        assertEquals(listOf(105f), r.guidesX)
        assertTrue(r.snapped)
    }

    @Test
    fun snapsCentreToNearestGuide() {
        // left=100, centre=150, right=200; target 148 is closest to the centre.
        val r = SnapEngine.snap(box(100f, 0f, 200f, 100f), listOf(148f), emptyList(), 10f)
        assertEquals(-2f, r.dx, 1e-3f)
        assertEquals(listOf(148f), r.guidesX)
    }

    @Test
    fun noSnapBeyondThreshold() {
        val r = SnapEngine.snap(box(100f, 0f, 200f, 100f), listOf(300f), listOf(400f), 10f)
        assertEquals(0f, r.dx, 1e-3f)
        assertEquals(0f, r.dy, 1e-3f)
        assertFalse(r.snapped)
    }

    @Test
    fun picksClosestAcrossEdges() {
        // left=100 near 103 (d3); right=200 near 205 (d5). Closest wins → shift +3, one guide.
        val r = SnapEngine.snap(box(100f, 0f, 200f, 100f), listOf(103f, 205f), emptyList(), 10f)
        assertEquals(3f, r.dx, 1e-3f)
        assertEquals(listOf(103f), r.guidesX)
    }

    @Test
    fun axesAreIndependent() {
        val r = SnapEngine.snap(box(100f, 100f, 200f, 200f), listOf(104f), listOf(97f), 10f)
        assertEquals(4f, r.dx, 1e-3f)   // left 100 → 104
        assertEquals(-3f, r.dy, 1e-3f)  // top 100 → 97
    }

    @Test
    fun guidesFromArtboardAndBoxes() {
        val (xs, ys) = SnapEngine.guidesFrom(box(0f, 0f, 1000f, 800f), listOf(box(200f, 100f, 400f, 300f)))
        // artboard: x {0,500,1000} y {0,400,800}; box: x {200,300,400} y {100,200,300}
        assertTrue(xs.containsAll(listOf(0f, 500f, 1000f, 200f, 300f, 400f)))
        assertTrue(ys.containsAll(listOf(0f, 400f, 800f, 100f, 200f, 300f)))
    }
}
