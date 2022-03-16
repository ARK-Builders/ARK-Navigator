package space.taran.arknavigator.mvp.model.repo.extra

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import space.taran.arknavigator.mvp.model.repo.index.MetaExtraTag
import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import space.taran.arknavigator.utils.RESOURCES_INDEX
import space.taran.arknavigator.utils.LogTags.RESOURCES_INDEX
import space.taran.arknavigator.utils.extension
import java.io.FileNotFoundException
import java.nio.file.Path

object DocumentMetaExtra {
    val ACCEPTED_EXTENSIONS: Set<String> =
        setOf("pdf", "txt", "doc", "docx", "odt", "ods", "md")

    fun extract(path: Path): ResourceMetaExtra? {
        val result = mutableMapOf<MetaExtraTag, String>()
        if (extension(path) != "pdf") return null

        var parcelFileDescriptor: ParcelFileDescriptor? = null

        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(
                path.toFile(),
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        } catch (e: FileNotFoundException) {
            Log.e(
                RESOURCES_INDEX,
                "Failed to find file at path: $path"
            )
        }
        parcelFileDescriptor ?: return null
        val pdfRenderer = PdfRenderer(parcelFileDescriptor)
        val totalPages = pdfRenderer.pageCount
        if (totalPages > 0) {
            result[MetaExtraTag.PAGES] = totalPages.toString()
            return ResourceMetaExtra(result)
        }
        return null
    }
}
