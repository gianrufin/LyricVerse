package com.example.media

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Extracts luminous, AMOLED-readable light color stops from album artwork bitmaps.
 * Ensures colors remain legible as text gradients on pure #000000 backgrounds.
 */
object AlbumArtColorExtractor {

    private val DEFAULT_STAGE_LIGHT_GRADIENT = listOf(0xFFE0E7FFL, 0xFFF5D0FEL, 0xFFBAE6FDL)

    fun extractLightGradientColors(bitmap: Bitmap?): List<Long> {
        if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return DEFAULT_STAGE_LIGHT_GRADIENT
        }
        val scaled = if (bitmap.width > 24 || bitmap.height > 24) {
            Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        } else {
            bitmap
        }
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        return extractLightGradientFromArgbPixels(pixels)
    }

    /**
     * Pure-JVM testable pixel quantization & luminance-lifting pipeline.
     * Groups pixels into hue buckets, lifts dark tones into vibrant pastel/light shades,
     * and returns 2-3 cohesive gradient stops (ARGB packed into Long).
     */
    fun extractLightGradientFromArgbPixels(pixels: IntArray): List<Long> {
        if (pixels.isEmpty()) return DEFAULT_STAGE_LIGHT_GRADIENT

        // 12 hue buckets (30 degrees each) + neutral bucket (index 12)
        val bucketCounts = IntArray(13)
        val bucketHueSum = FloatArray(13)
        val bucketSatSum = FloatArray(13)
        val bucketLightSum = FloatArray(13)

        val hsl = FloatArray(3)
        for (argb in pixels) {
            val alpha = (argb ushr 24) and 0xFF
            if (alpha < 128) continue
            rgbToHsl(
                r = (argb ushr 16) and 0xFF,
                g = (argb ushr 8) and 0xFF,
                b = argb and 0xFF,
                outHsl = hsl
            )
            // Skip near-pure black pixels so dark album borders don't wash out the palette
            if (hsl[2] < 0.08f) continue

            val bucket = if (hsl[1] < 0.14f) {
                12
            } else {
                ((hsl[0] / 30f).toInt()).coerceIn(0, 11)
            }
            // Prefer brighter, more colorful artwork regions
            val weight = if (hsl[2] > 0.45f && hsl[1] > 0.25f) 3 else 1
            bucketCounts[bucket] += weight
            bucketHueSum[bucket] += hsl[0] * weight
            bucketSatSum[bucket] += hsl[1] * weight
            bucketLightSum[bucket] += hsl[2] * weight
        }

        val rankedBuckets = (0..11)
            .filter { bucketCounts[it] > 0 }
            .sortedByDescending { bucketCounts[it] }

        if (rankedBuckets.isEmpty()) {
            return DEFAULT_STAGE_LIGHT_GRADIENT
        }

        val primaryIdx = rankedBuckets.first()
        val secondaryIdx = rankedBuckets.firstOrNull { idx ->
            val hueDist = abs(idx - primaryIdx)
            val circularDist = min(hueDist, 12 - hueDist)
            circularDist >= 2
        } ?: primaryIdx

        val primaryColor = buildLuminousStop(
            hue = bucketHueSum[primaryIdx] / bucketCounts[primaryIdx],
            sat = bucketSatSum[primaryIdx] / bucketCounts[primaryIdx],
            light = bucketLightSum[primaryIdx] / bucketCounts[primaryIdx],
        )

        val secondaryColor = if (secondaryIdx != primaryIdx) {
            buildLuminousStop(
                hue = bucketHueSum[secondaryIdx] / bucketCounts[secondaryIdx],
                sat = bucketSatSum[secondaryIdx] / bucketCounts[secondaryIdx],
                light = bucketLightSum[secondaryIdx] / bucketCounts[secondaryIdx],
            )
        } else {
            val shiftedHue = (bucketHueSum[primaryIdx] / bucketCounts[primaryIdx] + 38f) % 360f
            buildLuminousStop(
                hue = shiftedHue,
                sat = (bucketSatSum[primaryIdx] / bucketCounts[primaryIdx]).coerceIn(0.45f, 0.82f),
                light = 0.84f,
            )
        }

        val tertiaryColor = buildLuminousStop(
            hue = ((bucketHueSum[primaryIdx] / bucketCounts[primaryIdx]) + 18f) % 360f,
            sat = 0.55f,
            light = 0.89f,
        )

        return listOf(primaryColor, secondaryColor, tertiaryColor)
    }

    fun fallbackGradientForSignature(signature: String): List<Long> {
        if (signature.isBlank()) return DEFAULT_STAGE_LIGHT_GRADIENT
        val hash = abs(signature.hashCode())
        val baseHue = (hash % 360).toFloat()
        val c1 = hslToArgb(baseHue, 0.72f, 0.78f)
        val c2 = hslToArgb((baseHue + 42f) % 360f, 0.76f, 0.84f)
        val c3 = hslToArgb((baseHue + 78f) % 360f, 0.65f, 0.88f)
        return listOf(c1, c2, c3)
    }

    private fun buildLuminousStop(hue: Float, sat: Float, light: Float): Long {
        // Lift dark/muted album hues into airy, high-contrast light colors for pure #000000 AMOLED text
        val liftedSat = sat.coerceIn(0.42f, 0.86f)
        val liftedLight = light.coerceIn(0.73f, 0.88f)
        return hslToArgb(hue, liftedSat, liftedLight)
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int, outHsl: FloatArray) {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val maxC = max(rf, max(gf, bf))
        val minC = min(rf, min(gf, bf))
        val delta = maxC - minC

        val l = (maxC + minC) / 2f
        val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
        val h = when {
            delta == 0f -> 0f
            maxC == rf -> 60f * (((gf - bf) / delta) % 6f)
            maxC == gf -> 60f * (((bf - rf) / delta) + 2f)
            else -> 60f * (((rf - gf) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        outHsl[0] = h
        outHsl[1] = s.coerceIn(0f, 1f)
        outHsl[2] = l.coerceIn(0f, 1f)
    }

    private fun hslToArgb(h: Float, s: Float, l: Float): Long {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }
}
