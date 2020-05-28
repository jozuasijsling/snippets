package jozua.sijsling.snippets.ext

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// [snippet comments]
// Useful helper functions for a project where the visual properties were backend-driven.


// Follows the same conversion mechanism as in TypedValue.complexToDimensionPixelSize as used
// when setting padding. It rounds off the float value unless the value is < 1.
// When a value is between 0 and 1, it is set to 1. A value less than 0 is set to -1.
fun pixelsToDimensionPixelSize(pixels: Float): Int {
    val result = (pixels + 0.5f).toInt()
    return when {
        result != 0 -> result
        pixels == 0f -> 0
        pixels > 0 -> 1
        else -> -1
    }
}

val View.screenDensity get() = resources.displayMetrics.density
fun View.dpToPx(pixels: Int) = pixelsToDimensionPixelSize(pixels * screenDensity)
fun View.dpToPx(pixels: Float) = pixelsToDimensionPixelSize(pixels * screenDensity)
fun View.toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

/** Iterates through parents from self to root */
fun View.ancestry(includeSelf: Boolean = true): Iterable<View> = Iterable {
    object : Iterator<View> {
        var current: View? = if (includeSelf) this@ancestry else parent as? View
        override fun hasNext() = current != null
        override fun next(): View {
            val temp = current
            current = current?.parent as? View
            return temp!!
        }
    }
}

/** Finds activity from the context hierarchy */
fun View.findActivity(): AppCompatActivity = context.ancestry()
    .first { it is AppCompatActivity } as AppCompatActivity
