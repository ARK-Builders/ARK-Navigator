package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfiumCore
import space.taran.arknavigator.ui.App
import java.nio.file.Path

object PdfPreviewGenerator {
    fun generate(source: Path): Bitmap {
        val page = 0

        val finalContext = App.instance

        val pdfiumCore = PdfiumCore(finalContext)
        val fd: ParcelFileDescriptor? =
            finalContext
                .contentResolver
                .openFileDescriptor(Uri.fromFile(source.toFile()), "r")

        val document = pdfiumCore.newDocument(fd)
        pdfiumCore.openPage(document, page)

        val width = pdfiumCore.getPageWidthPoint(document, page)
        val height = pdfiumCore.getPageHeightPoint(document, page)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        pdfiumCore.renderPageBitmap(document, bitmap, page, 0, 0, width, height)
        pdfiumCore.closeDocument(document)

        return bitmap
    }
}
