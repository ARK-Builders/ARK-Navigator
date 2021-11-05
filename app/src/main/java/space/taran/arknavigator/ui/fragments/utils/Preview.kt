package space.taran.arknavigator.ui.fragments.utils

import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.ResourceMeta
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.isFormat
import space.taran.arknavigator.utils.isImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class Preview(
    val imagePath: Path,
    val isZoomEnabled: Boolean = false,         //todo: zoomable
    val isPlayButtonVisible: Boolean = false,   //todo: move into other place later
) {
    companion object {
        private val PRIMARY_STORAGE: Path =
            Paths.get("${App.instance.cacheDir}/previews")

        init {
            Files.createDirectory(PRIMARY_STORAGE)
        }

        private fun imagePath(id: ResourceId): Path =
            PRIMARY_STORAGE.resolve(id.toString())

        fun forget(id: ResourceId) {
            Files.delete(imagePath(id))
        }

//        fun provide(path: Path): Preview {
//
//        }

        fun provide(path: Path, meta: ResourceMeta): Preview {
            if (Files.isDirectory(path)) {
                throw AssertionError("Previews for folders are constant")
            }

            val imagePath = imagePath(meta.id)

            if (Files.exists(imagePath)) {
                return Preview(imagePath)
            }

            //todo generating only when size > 5mb?
            if (isFormat(path, "pdf")) {
                generatePdfPreview(path, imagePath)
                return Preview(imagePath, isZoomEnabled = true)
            }
            if (isImage(path)) {
                return Preview(path, isZoomEnabled = true)
            }

            //todo
            //meta.extra?.type == ResourceType.VIDEO
            //isZoomEnabled = false
            //isPlayButtonVisible = false

            throw AssertionError("")
        }

        private fun generatePdfPreview(source: Path, target: Path) {
            val page = 0
            val quality = 100

            val finalContext = App.instance

            val pdfiumCore = PdfiumCore(finalContext)
            val fd: ParcelFileDescriptor? =
                finalContext.contentResolver.openFileDescriptor(Uri.fromFile(source.toFile()), "r")

            val document = pdfiumCore.newDocument(fd)
            pdfiumCore.openPage(document, page)

            val width = pdfiumCore.getPageWidthPoint(document, page)
            val height = pdfiumCore.getPageHeightPoint(document, page)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            pdfiumCore.renderPageBitmap(document, bitmap, page, 0, 0, width, height)
            pdfiumCore.closeDocument(document)

            Files.newOutputStream(target).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, quality, out)
                out.flush()
            }
        }
    }
}