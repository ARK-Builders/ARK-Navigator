package space.taran.arknavigator.mvp.model.repo.kind

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.utils.LogTags.RESOURCES_INDEX
import space.taran.arknavigator.utils.extension
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Path

object DocumentKindFactory : ResourceKindFactory<ResourceKind.Document> {
    override val acceptedExtensions: Set<String> =
        setOf("pdf", "doc", "docx", "odt", "ods", "md")
    override val acceptedMimeTypes: Set<String>
        get() = setOf("application/pdf")
    override val acceptedKindCode = KindCode.DOCUMENT

    @Throws(IOException::class)
    override fun fromPath(path: Path): ResourceKind.Document {
        if (extension(path) != "pdf") return ResourceKind.Document()

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
        parcelFileDescriptor ?: return ResourceKind.Document()
        val pdfRenderer = PdfRenderer(parcelFileDescriptor)
        val totalPages = pdfRenderer.pageCount
        val pages = if (totalPages > 0) totalPages else null

        return ResourceKind.Document(pages)
    }

    override fun fromRoom(extras: Map<MetaExtraTag, String>): ResourceKind.Document =
        ResourceKind.Document(extras[MetaExtraTag.PAGES]?.toInt())

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.Document
    ): Map<MetaExtraTag, String?> = mapOf(
        MetaExtraTag.PAGES to kind.pages?.toString()
    )
}
