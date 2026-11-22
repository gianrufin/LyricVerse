package com.example

import com.example.media.AlbumArtColorExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumArtColorExtractorTest {

    @Test
    fun `extracts lifted luminous light stops from dark album pixels for AMOLED contrast`() {
        // Dark deep navy & deep crimson album pixels
        val darkNavy = 0xFF0A1432.toInt()
        val deepCrimson = 0xFF3A0C1E.toInt()
        val pixels = IntArray(40) { idx -> if (idx % 3 == 0) deepCrimson else darkNavy }

        val colors = AlbumArtColorExtractor.extractLightGradientFromArgbPixels(pixels)
        assertEquals(3, colors.size)

        // Every extracted stop must be lifted to a bright/light pastel shade readable on #000000
        colors.forEach { argbLong ->
            val argb = argbLong.toInt()
            val r = (argb ushr 16) and 0xFF
            val g = (argb ushr 8) and 0xFF
            val b = argb and 0xFF
            val maxChannel = maxOf(r, maxOf(g, b))
            assertTrue("Expected bright light stop, got R=$r G=$g B=$b", maxChannel >= 190)
        }
    }

    @Test
    fun `fallback gradient is deterministic per track signature and distinct across songs`() {
        val g1 = AlbumArtColorExtractor.fallbackGradientForSignature("artist a|midnight glow|album")
        val g1Repeat = AlbumArtColorExtractor.fallbackGradientForSignature("artist a|midnight glow|album")
        val g2 = AlbumArtColorExtractor.fallbackGradientForSignature("artist b|golden hour|album")

        assertEquals(g1, g1Repeat)
        assertEquals(3, g1.size)
        assertNotEquals(g1, g2)
    }
}
