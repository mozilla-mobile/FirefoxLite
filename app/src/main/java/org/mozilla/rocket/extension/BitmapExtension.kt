package org.mozilla.rocket.extension

import android.graphics.Bitmap

fun Bitmap.obtainBackgroundColor(): Int {
    val palette = androidx.palette.graphics.Palette.from(this).generate()
    var maxPopulation = 0
    var bodyColor = 0
    for (swatch in palette.swatches) {
        if (swatch.population > maxPopulation) {
            maxPopulation = swatch.population
            bodyColor = swatch.rgb
        }
    }
    return bodyColor
}