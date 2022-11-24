package com.tbox.fotki.util.upload_files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.tbox.fotki.util.compression.Compressor
import java.io.File
import java.io.IOException

object ImageHelper {
    private var threshold = 1400

    fun compress(context: Context, sourceFile: File?):File?{
        if (sourceFile != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(sourceFile!!.absolutePath)
                var width = bitmap.width
                var height = bitmap.height
                var max = width
                max = if (max > height) {
                    width
                } else {
                    height
                }
                if (max > threshold) {
                    // todo get proportion
                    var proportion = getProportion(max = max)
                    proportion /= 100
                    val percentWidth = proportion * width
                    val percentHeight = proportion * height
                    width -= percentWidth.toInt()
                    height -= percentHeight.toInt()
                    // Compress image in main thread using custom Compressor
                    return Compressor(context)
                        .setMaxWidth(width)
                        .setMaxHeight(height)
                        .setQuality(75)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .setDestinationDirectoryPath(
                            Environment.getExternalStorageDirectory().toString()
                                    + File.separator + "FotkiUploader"
                        )
                        .compressToFile(sourceFile)
                } else {
                    return sourceFile
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }

        } else {
            return null
        }
    }

    private fun getProportion(max: Int): Float {
        var proportion = 1400.0f / max
        proportion *= 100
        proportion = 100 - proportion
        return proportion
    }
}