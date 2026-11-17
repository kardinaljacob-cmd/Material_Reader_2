package com.example.data.parser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfBookHelper {

  data class PdfMetadata(
    val pageCount: Int,
    val coverBytes: ByteArray?,
  )

  fun inspectPdf(file: File): PdfMetadata {
    return runCatching {
      ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
          val count = renderer.pageCount.coerceAtLeast(1)
          val coverBytes =
            if (renderer.pageCount > 0) {
              renderer.openPage(0).use { page ->
                val targetW = 480
                val targetH = ((page.height.toFloat() / page.width.coerceAtLeast(1)) * targetW)
                  .toInt()
                  .coerceIn(240, 800)
                val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                Canvas(bmp).drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val baos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                bmp.recycle()
                baos.toByteArray()
              }
            } else {
              null
            }
          PdfMetadata(pageCount = count, coverBytes = coverBytes)
        }
      }
    }.getOrDefault(PdfMetadata(pageCount = 1, coverBytes = null))
  }

  suspend fun renderPage(
    filePath: String,
    pageIndex: Int,
    targetWidthPx: Int = 1280,
  ): Bitmap? =
    withContext(Dispatchers.IO) {
      runCatching {
        val file = File(filePath)
        if (!file.exists()) return@withContext null
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
          PdfRenderer(pfd).use { renderer ->
            if (pageIndex !in 0 until renderer.pageCount) return@withContext null
            renderer.openPage(pageIndex).use { page ->
              val w = targetWidthPx.coerceIn(600, 1800)
              val h =
                ((page.height.toFloat() / page.width.coerceAtLeast(1)) * w)
                  .toInt()
                  .coerceIn(400, 2600)
              val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
              Canvas(bmp).drawColor(Color.WHITE)
              page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
              bmp
            }
          }
        }
      }.getOrNull()
    }
}
