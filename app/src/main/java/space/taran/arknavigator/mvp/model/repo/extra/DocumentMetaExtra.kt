package space.taran.arknavigator.mvp.model.repo.extra

import android.os.ParcelFileDescriptor
import java.nio.file.Path
import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import android.graphics.pdf.PdfRenderer
import space.taran.arknavigator.mvp.model.repo.index.MetaExtraTag
import space.taran.arknavigator.utils.extension
import java.io.FileNotFoundException

object DocumentMetaExtra {
    val ACCEPTED_EXTENSIONS: Set<String> =
        setOf("pdf", "txt", "doc", "docx", "odt", "ods", "md")

    fun extract(path: Path): ResourceMetaExtra? {
        val result = mutableMapOf<MetaExtraTag, Long>()
        if (extension(path) != "pdf") return null
        try {
            val parcelFileDescriptor = ParcelFileDescriptor.open(
                path.toFile(),
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            val totalPages = pdfRenderer.pageCount
            if (totalPages > 0) {
                result[MetaExtraTag.PAGES] = totalPages.toLong()
                return ResourceMetaExtra(result)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return null
    }
}
