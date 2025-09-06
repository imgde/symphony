package io.github.zyrouge.symphony.utils

import android.graphics.Bitmap
import androidx.core.graphics.scale
import kotlin.math.max

object ImagePreserver {
    enum class Quality(val maxSide: Int?) {
        Low(256),
        Medium(512),
        High(1024),
        Loseless(null),
    }

    fun resize(bitmap: Bitmap, quality: Quality): Bitmap {
        if (quality.maxSide == null || max(bitmap.width, bitmap.height) < quality.maxSide) {
            return bitmap
        }
        val (width, height) = calculateDimensions(bitmap.width, bitmap.height, quality.maxSide)
        return bitmap.scale(width, height)
    }

    private fun calculateDimensions(width: Int, height: Int, maxSide: Int) = when {
        width > height -> maxSide to (height * (maxSide.toFloat() / width)).toInt()
        width < height -> (width * (maxSide.toFloat() / height)).toInt() to maxSide
        else -> maxSide to maxSide
    }
}
