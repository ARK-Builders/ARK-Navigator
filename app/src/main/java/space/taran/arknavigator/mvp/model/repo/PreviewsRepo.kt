package space.taran.arknavigator.mvp.model.repo

import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import com.ortiz.touchview.TouchImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.*
import space.taran.arknavigator.utils.extensions.autoDisposeScope
import space.taran.arknavigator.utils.extensions.makeGone
import space.taran.arknavigator.utils.extensions.textOrGone
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import com.shockwave.pdfium.PdfiumCore

class PreviewsRepo {

    fun providePreview(path: Path): Preview {
        if (Files.isDirectory(path)) {
            return Preview(filePath = path, isFolder = true)
        }
        return createPreview(path)
    }

    private fun createPreview(file: Path): Preview {
        return if (isPDF(file)) getPdfPreview(file)
        else Preview(filePath = file, fileExtension = extension(file))
    }

    private fun getPdfPreview(file: Path): Preview {

        val encodedID = Base64.encodeToString(file.toString().toByteArray(), Base64.URL_SAFE)
        val savedPreviews = getSavedPdfPreviewsDecoded()

        return if (savedPreviews?.contains(encodedID) == true) {
            Preview(
                filePath = getPdfPreviewPathByID(encodedID),
                fileExtension = extension(file)
            )
        } else Preview(
            filePath = file,
            fileExtension = extension(file)
        )
    }

    fun generatePdfPreviewsForMeta(metaByPath: MutableMap<Path, ResourceMeta>) {
        val previewsFolder = getPdfPreviewsFolder()
        val savedPreviews = getSavedPdfPreviewsDecoded()

        metaByPath.forEach {
            val path = it.key
            var out: FileOutputStream? = null

            val encodedID = Base64.encodeToString(path.toString().toByteArray(), Base64.URL_SAFE)

            if (savedPreviews == null || !savedPreviews.contains(encodedID)) {
                if (isPDF(path) && it.value.size / MEGABYTE >= 5) {
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
    }

    fun loadPreview(
        targetView: ImageView,
        preview: Preview,
        extraMeta: ResourceMetaExtra?,
        centerCrop: Boolean = false
    ) {

        val filePath = preview.filePath
        val fileExtension = preview.fileExtension

        when (extraMeta?.type()) {
            ResourceType.GIF -> {
                if (targetView is TouchImageView){
                    loadGif(
                        filePath,
                        targetView
                    )
                }
                else {
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
                        if (isPDF(filePath)) {
                            val bitmap =
                                createPdfPreview(filePath)
                            withContext(Dispatchers.Main) {
                                loadCroppedBitmap(bitmap, targetView)
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

    fun isPlayButtonVisible(preview: Preview) = isVideo(preview.filePath)

    fun isZoomEnabled(preview: Preview, extraMeta: ResourceMetaExtra?) =
        preview.isFolder == null &&
                extraMeta?.type() != ResourceType.PDF &&
                extraMeta?.type() != ResourceType.VIDEO

    fun loadExtraMeta(
        extraMeta: ResourceMetaExtra?,
        resolutionTV: TextView? = null,
        durationTV: TextView? = null
    ) {
        if (extraMeta != null) {
            val extraInfoMap = extraMeta.appData()

            resolutionTV.textOrGone(extraInfoMap[Preview.ExtraInfoTag.MEDIA_RESOLUTION])
            durationTV.textOrGone(extraInfoMap[Preview.ExtraInfoTag.MEDIA_DURATION])
            return
        } else {
            resolutionTV?.makeGone()
            durationTV?.makeGone()
        }
    }

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