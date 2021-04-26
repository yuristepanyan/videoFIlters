package com.video.filters

import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.daasuu.gpuv.egl.filter.GlRGBFilter

/**
 * convert CLA to RGB
 *
 * @param a is blue to yellow filter
 * @param b is green to red filter
 * @return GlRGBFilter which can be used to camera filter
 */
fun getRGBFromLab(a: Double, b: Double): GlRGBFilter {
    val xyz = DoubleArray(3)
    ColorUtils.LABToXYZ(100.toDouble(), a, b, xyz)
    val color = ColorUtils.XYZToColor(xyz[0], xyz[1], xyz[2])

    return  GlRGBFilter().apply {
        setRed(color.red.toFloat()/256)
        setBlue(color.blue.toFloat()/256)
        setGreen(color.green.toFloat()/256)
    }
}