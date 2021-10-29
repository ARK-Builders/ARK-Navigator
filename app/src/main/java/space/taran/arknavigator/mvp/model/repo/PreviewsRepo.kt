package space.taran.arknavigator.mvp.model.repo

import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import com.ortiz.touchview.TouchImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import com.shockwave.pdfium.PdfiumCore
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.utils.extensions.*

class PreviewsRepo {

    fun providePreview(path: Path, meta: ResourceMeta? = null): Preview {
        if (Files.isDirectory(path)) {
            return Preview(filePath = path, isFolder = true)
        }
        return if (isFormat(path, "pdf")) getPdfPreview(path, meta?.id)
        else Preview(
            filePath = path,
            fileExtension = extension(path),
            isZoomEnabled = meta?.extra?.type(path) != ResourceType.VIDEO,
            isPlayButtonVisible = isVideo(path)
        )
    }

    private fun getPdfPreview(file: Path, id: ResourceId?): Preview {

        val encodedID = id?.toString() ?: ""
        val savedPreviews = getSavedPdfPreviews()

        return if (savedPreviews?.contains(encodedID) == true) {
            Preview(
                filePath = encodedID.toPdfPreviewPath(),
                fileExtension = extension(file)
            )
        } else Preview(
            filePath = file,
            fileExtension = extension(file)
        )
    }

    fun generatePdfPreview(path: Path, meta: ResourceMeta) {
        val previewsFolder = getPdfPreviewsFolder()
        val savedPreviews = getSavedPdfPreviews()

        var out: FileOutputStream? = null

        val encodedID = meta.id.toString()

        if (savedPreviews == null || !savedPreviews.contains(encodedID)) {
            if (meta.size / MEGABYTE >= 5) {
                try {
                    if (!previewsFolder.exists()) previewsFolder.mkdirs()

                    val file = File(previewsFolder, "$encodedID.png")
                    out = FileOutputStream(file)
                    createPdfPreview(path)
                        ?.compress(Bitmap.CompressFormat.PNG, 100, out)
                } catch (e: Exception) {
                } finally {
                    try {
                        out?.close()
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    fun deletePdfPreviews(idList: List<String>) {
        val savedPreviews = getSavedPdfPreviews() ?: return
        idList.forEach { item ->
            val foundPreview = savedPreviews.find { it == item }

            if (!foundPreview.isNullOrEmpty()) {
                deleteFile(foundPreview.toPdfPreviewPath())
            }
        }
    }

    fun loadPreview(
        targetView: ImageView,
        preview: Preview,
        extraMeta: ResourceMetaExtra?,
        centerCrop: Boolean = false
    ) {
        val filePath = preview.filePath
        val fileExtension = preview.fileExtension

        when (extraMeta?.type(filePath)) {
            ResourceType.GIF -> {
                if (targetView is TouchImageView) {
                    loadGif(
                        filePath,
                        targetView
                    )
                } else {
                    loadGifThumbnailWithPlaceHolder(
                        filePath,
                        imageForPredefinedExtension("gif"),
                        targetView
                    )
                }
            }
            ResourceType.PDF -> {
                targetView.setImageResource(imageForPredefinedExtension("pdf"))
                targetView.autoDisposeScope.launch {
                    withContext(Dispatchers.IO) {
                        if (isFormat(filePath, "pdf")) {
                            val bitmap =
                                createPdfPreview(filePath)
                            withContext(Dispatchers.Main) {
                                if (centerCrop)
                                    loadCroppedBitmap(bitmap, targetView)
                                else loadBitmap(bitmap, targetView)
                            }
                        } else withContext(Dispatchers.Main) {
                            loadZoomImage(filePath, targetView)
                        }
                    }
                }
            }
            else -> {
                if (fileExtension != null && filePath != null) {
                    if (centerCrop) {
                        loadCroppedImageWithPlaceHolder(
                            filePath,
                            imageForPredefinedExtension(fileExtension),
                            targetView
                        )
                    } else loadImageWithPlaceHolder(
                        filePath,
                        imageForPredefinedExtension(fileExtension),
                        targetView
                    )
                } else if (fileExtension != null)
                    targetView.setImageResource(imageForPredefinedExtension(preview.fileExtension))
                else {
                    if (centerCrop) loadCroppedImage(
                        filePath!!,
                        targetView
                    )
                    else loadImageWithTransition(filePath, targetView)
                }
            }
        }
    }

    fun loadExtraMeta(
        extraMeta: ResourceMetaExtra? = null,
        extraInfo: MutableMap<Preview.ExtraInfoTag, String>? = null,
        filePath: Path?,
        infoTag: Preview.ExtraInfoTag
    ): String? =
        (extraMeta?.appData(filePath) ?: extraInfo)
            ?.getOrDefault(infoTag, null)

    private fun createPdfPreview(filePath: Path?): Bitmap? {
        val pageNumber = 0
        val finalContext = App.instance

        val pdfiumCore = PdfiumCore(finalContext)
        val fd: ParcelFileDescriptor? =
            finalContext.contentResolver.openFileDescriptor(Uri.fromFile(filePath?.toFile()), "r")

        val pdfDocument = pdfiumCore.newDocument(fd)
        pdfiumCore.openPage(pdfDocument, pageNumber)

        val width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber)
        val height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height)
        pdfiumCore.closeDocument(pdfDocument)
        return bmp
    }
}