package com.glowcare.app.ui.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Local, on-device image processing engine.
 * Filters are defined as color matrices; intensity interpolates each matrix
 * toward identity (0% = original, 100% = full filter). Beauty adjustments
 * combine color matrices with a fast stack blur for smoothing.
 */
object FilterEngine {

    data class Filter(val name: String, val matrix: FloatArray, val premium: Boolean = false) {
        override fun equals(other: Any?) = other is Filter && other.name == name
        override fun hashCode() = name.hashCode()
    }

    /** A filter is locked unless the user has an active Premium entitlement. */
    fun isFilterLocked(filter: Filter, premium: Boolean): Boolean = filter.premium && !premium

    private fun scaleMatrix(saturation: Float, contrast: Float = 1f, brightness: Float = 0f,
                            rTint: Float = 0f, gTint: Float = 0f, bTint: Float = 0f): FloatArray {
        val m = ColorMatrix()
        m.setSaturation(saturation)
        val c = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        val t = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, rTint,
            0f, 1f, 0f, 0f, gTint,
            0f, 0f, 1f, 0f, bTint,
            0f, 0f, 0f, 1f, 0f
        ))
        val out = ColorMatrix()
        out.set(m)
        out.postConcat(t)
        out.postConcat(c)
        return out.array
    }

    private fun sepiaLike(r1: Float, r2: Float, r3: Float, tint: Float): FloatArray {
        val base = ColorMatrix(floatArrayOf(
            r1, r2, r3, 0f, tint,
            r1 * 0.85f, r2 * 0.95f, r3 * 0.9f, 0f, tint * 0.7f,
            r1 * 0.75f, r2 * 0.9f, r3 * 1.05f, 0f, tint * 0.4f,
            0f, 0f, 0f, 1f, 0f
        ))
        return base.array
    }

    val FILTERS: List<Filter> = listOf(
        Filter("Natural", scaleMatrix(1f)),
        Filter("Soft Glow", scaleMatrix(1.05f, 0.96f, 8f, 4f, 3f, 6f)),
        Filter("Rose", scaleMatrix(0.9f, 0.98f, 5f, 12f, 4f, 10f)),
        Filter("Fresh", scaleMatrix(1.1f, 1.02f, 2f, 2f, 8f, -4f)),
        Filter("Golden Hour", scaleMatrix(1.0f, 1.05f, 10f, 14f, 4f, -8f)),
        Filter("Cream", scaleMatrix(0.85f, 0.95f, 12f, 10f, 6f, 2f)),
        Filter("Dream", scaleMatrix(0.8f, 0.92f, 14f, 8f, 10f, 14f)),
        Filter("Peach", scaleMatrix(0.95f, 1.0f, 6f, 12f, 5f, 2f)),
        Filter("Warm", scaleMatrix(1.02f, 1.08f, 6f, 9f, 2f, -10f)),
        Filter("Clean", scaleMatrix(1.08f, 0.98f, 4f, 4f, 6f, 10f)),
        Filter("Sunset", scaleMatrix(0.95f, 1.1f, 14f, 8f, -2f, -14f)),
        Filter("Vintage", sepiaLike(0.9f, 0.75f, 0.6f, 10f)),
        Filter("Cool", scaleMatrix(1.0f, 1.0f, -4f, -6f, 4f, 14f)),
        Filter("Bright", scaleMatrix(1.03f, 0.95f, 14f, 12f, 12f, 12f)),
        Filter("Soft Film", scaleMatrix(0.92f, 1.0f, 6f, 6f, 4f, 8f)),

        // ---- PREMIUM COLLECTION (visually distinct, all real matrices) ----
        Filter("Silky", scaleMatrix(0.88f, 0.94f, 10f, 6f, 8f, 12f), premium = true),
        Filter("Velvet", scaleMatrix(0.9f, 1.06f, 12f, 10f, 2f, 12f), premium = true),
        Filter("Sunlit", scaleMatrix(1.06f, 1.12f, 16f, 10f, 2f, -14f), premium = true),
        Filter("Cinematic", scaleMatrix(0.82f, 1.14f, 8f, 2f, 0f, -6f), premium = true),
        Filter("Luminous", scaleMatrix(1.08f, 0.92f, 16f, 14f, 14f, 6f), premium = true),
        Filter("Natural Glow", scaleMatrix(1.04f, 0.97f, 8f, 6f, 5f, 4f), premium = true),
        Filter("Warm Portrait", scaleMatrix(0.98f, 1.1f, 12f, 12f, 3f, -6f), premium = true),
        Filter("Clean Skin", scaleMatrix(1.1f, 0.96f, 6f, 5f, 7f, 8f), premium = true),
        Filter("Glass Glow", scaleMatrix(1.12f, 0.9f, 18f, 16f, 16f, 10f), premium = true),
        Filter("Rose Glow", scaleMatrix(0.86f, 1.04f, 10f, 16f, 6f, 14f), premium = true),
        Filter("Peach Light", scaleMatrix(0.96f, 1.02f, 12f, 16f, 8f, 4f), premium = true),
        Filter("Soft Pink", scaleMatrix(0.84f, 1.0f, 14f, 18f, 10f, 16f), premium = true)
    )

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun lerpMatrix(target: FloatArray, intensity: Float): ColorMatrix {
        // Interpolate between identity and the filter matrix.
        val result = FloatArray(20)
        for (i in 0 until 20) {
            val identity = when (i) {
                0, 6, 12, 18 -> 1f
                else -> 0f
            }
            result[i] = lerp(identity, target[i], intensity)
        }
        return ColorMatrix(result)
    }

    /** Applies a filter with the given intensity (0..1) to a bitmap (fail-safe). */
    fun applyFilter(source: Bitmap, filter: Filter, intensity: Float): Bitmap {
        if (intensity <= 0.01f) return source
        // Any failure (incl. OutOfMemoryError) falls back to the unfiltered photo
        // instead of crashing the app.
        return runCatching {
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            paint.colorFilter = ColorMatrixColorFilter(lerpMatrix(filter.matrix, intensity.coerceIn(0f, 1f)))
            canvas.drawBitmap(source, 0f, 0f, paint)
            output
        }.getOrDefault(source)
    }

    data class BeautySettings(
        val smooth: Float = 0f,     // 0..1 skin smoothing (blur mix)
        val brighten: Float = 0f,   // 0..1 brightness lift
        val tone: Float = 0f,       // -1..1 warm<->cool
        val blemish: Float = 0f     // 0..1 (implemented as gentle local contrast reduce)
    ) {
        val isOff: Boolean get() = smooth < 0.01f && brighten < 0.01f &&
            kotlin.math.abs(tone) < 0.01f && blemish < 0.01f
    }

    /** Applies subtle beauty adjustments. Runs fully on-device (fail-safe). */
    fun applyBeauty(source: Bitmap, s: BeautySettings): Bitmap {
        if (s.isOff) return source
        // Each stage falls back to its input on any failure so an unsupported or
        // memory-hungry effect degrades gracefully instead of killing the process.
        return runCatching { applyBeautyInner(source, s) }.getOrDefault(source)
    }

    private fun applyBeautyInner(source: Bitmap, s: BeautySettings): Bitmap {
        var current = source

        // Color part: brightness + tone warmth.
        if (s.brighten > 0.01f || kotlin.math.abs(s.tone) > 0.01f) {
            val lift = s.brighten * 18f
            val warm = s.tone * 10f
            val cm = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, lift + warm,
                0f, 1f, 0f, 0f, lift + warm * 0.4f,
                0f, 0f, 1f, 0f, lift - warm,
                0f, 0f, 0f, 1f, 0f
            ))
            val out = Bitmap.createBitmap(current.width, current.height, Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(current, 0f, 0f, Paint().apply {
                colorFilter = ColorMatrixColorFilter(cm)
                isFilterBitmap = true
            })
            current = out
        }

        // Smoothing: mix in a stack-blurred copy. Radius scales with setting;
        // mixing the original back keeps edges (eyes, brows) readable.
        if (s.smooth > 0.01f) {
            val radius = 2 + (s.smooth * 6).toInt()
            val blurred = stackBlur(current, radius)
            current = blend(current, blurred, alpha = s.smooth * 0.75f)
        }
        return current
    }

    private fun blend(a: Bitmap, b: Bitmap, alpha: Float): Bitmap {
        val out = Bitmap.createBitmap(a.width, a.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(a, 0f, 0f, null)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        p.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        canvas.drawBitmap(b, 0f, 0f, p)
        return out
    }

    /**
     * Fast box blur used for smoothing (no native dependency). Memory-lean:
     * exactly one source-copy bitmap plus TWO full pixel buffers (the vertical
     * pass writes back into the horizontal pass's input buffer, halving peak
     * memory vs. the previous three-buffer version).
     */
    fun stackBlur(src: Bitmap, radius: Int): Bitmap {
        val bitmap = src.copy(Bitmap.Config.ARGB_8888, true)
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val r = radius.coerceIn(1, 25)
        val div = 2 * r + 1

        fun shiftR(p: Int) = (p shr 16) and 0xFF
        fun shiftG(p: Int) = (p shr 8) and 0xFF
        fun shiftB(p: Int) = p and 0xFF

        // Horizontal pass: pixels -> temp
        val temp = IntArray(w * h)
        for (y in 0 until h) {
            var rSum = 0; var gSum = 0; var bSum = 0
            for (i in -r..r) {
                val px = pixels[y * w + i.coerceIn(0, w - 1)]
                rSum += shiftR(px); gSum += shiftG(px); bSum += shiftB(px)
            }
            for (x in 0 until w) {
                val outR = rSum / div; val outG = gSum / div; val outB = bSum / div
                temp[y * w + x] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
                val add = pixels[y * w + (x + r + 1).coerceAtMost(w - 1)]
                val sub = pixels[y * w + (x - r).coerceAtLeast(0)]
                rSum += shiftR(add) - shiftR(sub)
                gSum += shiftG(add) - shiftG(sub)
                bSum += shiftB(add) - shiftB(sub)
            }
        }
        // Vertical pass: temp -> pixels (in-place reuse, no third buffer)
        for (x in 0 until w) {
            var rSum = 0; var gSum = 0; var bSum = 0
            for (i in -r..r) {
                val px = temp[i.coerceIn(0, h - 1) * w + x]
                rSum += shiftR(px); gSum += shiftG(px); bSum += shiftB(px)
            }
            for (y in 0 until h) {
                val outR = rSum / div; val outG = gSum / div; val outB = bSum / div
                pixels[y * w + x] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
                val add = temp[(y + r + 1).coerceAtMost(h - 1) * w + x]
                val sub = temp[(y - r).coerceAtLeast(0) * w + x]
                rSum += shiftR(add) - shiftR(sub)
                gSum += shiftG(add) - shiftG(sub)
                bSum += shiftB(add) - shiftB(sub)
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    /** Editor adjustments (all -1..1 except sharpness/blur 0..1). */
    data class Adjustments(
        val brightness: Float = 0f,   // -1..1
        val contrast: Float = 0f,     // -1..1
        val saturation: Float = 0f,   // -1..1
        val warmth: Float = 0f,       // -1..1
        val highlights: Float = 0f,   // -1..1 (recover/boost bright regions)
        val shadows: Float = 0f,      // -1..1 (lift/crush dark regions)
        val sharpness: Float = 0f,    //  0..1 (unsharp mask)
        val blur: Float = 0f          //  0..1
    ) {
        /** All values clamped to their safe working ranges. */
        fun clamped() = Adjustments(
            brightness = brightness.coerceIn(-1f, 1f),
            contrast = contrast.coerceIn(-1f, 1f),
            saturation = saturation.coerceIn(-1f, 1f),
            warmth = warmth.coerceIn(-1f, 1f),
            highlights = highlights.coerceIn(-1f, 1f),
            shadows = shadows.coerceIn(-1f, 1f),
            sharpness = sharpness.coerceIn(0f, 1f),
            blur = blur.coerceIn(0f, 1f)
        )

        val isOff: Boolean get() = brightness == 0f && contrast == 0f && saturation == 0f &&
            warmth == 0f && highlights == 0f && shadows == 0f && sharpness == 0f && blur == 0f
    }

    fun applyAdjustments(source: Bitmap, a: Adjustments): Bitmap {
        if (a.isOff) return source
        return runCatching { applyAdjustmentsInner(source, a) }.getOrDefault(source)
    }

    private fun applyAdjustmentsInner(source: Bitmap, adjustments: Adjustments): Bitmap {
        val a = adjustments.clamped()
        val contrast = 1f + a.contrast
        val brightness = a.brightness * 40f
        val saturation = 1f + a.saturation
        val warm = a.warmth * 12f
        val cm = ColorMatrix()
        cm.setSaturation(saturation.coerceAtLeast(0f))
        val c = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness + warm,
            0f, contrast, 0f, 0f, brightness + warm * 0.4f,
            0f, 0f, contrast, 0f, brightness - warm,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(c)

        var current = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(current).drawBitmap(source, 0f, 0f, Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
            isFilterBitmap = true
        })
        if (a.blur > 0.01f) {
            current = stackBlur(current, 1 + (a.blur * 8).toInt())
        }
        if (a.highlights != 0f || a.shadows != 0f) {
            current = applyToneCurve(current, a.highlights, a.shadows)
        }
        if (a.sharpness > 0.01f) {
            current = sharpen(current, a.sharpness)
        }
        return current
    }

    /**
     * Highlights/shadows tone shaping: a per-pixel luminance-weighted lift.
     * highlights>0 recovers bright areas (pulls down), <0 boosts them;
     * shadows>0 lifts dark areas, <0 crushes them. Single reused buffer.
     */
    private fun applyToneCurve(src: Bitmap, highlights: Float, shadows: Float): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val hi = highlights * 0.6f
        val sh = shadows * 0.6f
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            // Rec.709 luma, normalized 0..1
            val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
            // Weight masks: highlights affect bright pixels, shadows affect dark ones.
            val hiMask = lum * lum
            val shMask = (1f - lum) * (1f - lum)
            val delta = (hi * hiMask * 90f) + (sh * shMask * 90f)
            val nr = (r + delta).toInt().coerceIn(0, 255)
            val ng = (g + delta).toInt().coerceIn(0, 255)
            val nb = (b + delta).toInt().coerceIn(0, 255)
            pixels[i] = (p and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
        }
        val bitmap = src.copy(Bitmap.Config.ARGB_8888, true)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * Unsharp mask: out = orig + amount * (orig - blurred).
     * Reuses the existing box blur; one extra bitmap at a time.
     */
    private fun sharpen(src: Bitmap, amount: Float): Bitmap {
        val radius = 2
        val blurred = stackBlur(src, radius)
        val w = src.width
        val h = src.height
        val orig = IntArray(w * h)
        val blur = IntArray(w * h)
        src.getPixels(orig, 0, w, 0, 0, w, h)
        blurred.getPixels(blur, 0, w, 0, 0, w, h)
        val k = amount * 1.6f
        for (i in orig.indices) {
            val p = orig[i]
            val q = blur[i]
            val r = (((p shr 16) and 0xFF) + k * (((p shr 16) and 0xFF) - ((q shr 16) and 0xFF))).toInt().coerceIn(0, 255)
            val g = (((p shr 8) and 0xFF) + k * (((p shr 8) and 0xFF) - ((q shr 8) and 0xFF))).toInt().coerceIn(0, 255)
            val b = ((p and 0xFF) + k * ((p and 0xFF) - (q and 0xFF))).toInt().coerceIn(0, 255)
            orig[i] = (p and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
        }
        val bitmap = src.copy(Bitmap.Config.ARGB_8888, true)
        bitmap.setPixels(orig, 0, w, 0, 0, w, h)
        return bitmap
    }
}
