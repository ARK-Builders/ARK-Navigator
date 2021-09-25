package space.taran.arknavigator.utils

import android.content.Context
import space.taran.arknavigator.ui.App
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import space.taran.arknavigator.mvp.model.dao.computeId
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension

val ROOT_PATH: Path = Paths.get("/")

val ANDROID_DIRECTORY: Path = Paths.get("Android")

typealias Milliseconds = Long
typealias StringPath = String

enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}

enum class FileActionType{
    EDIT_AS_OPEN, EDIT_AND_OPEN, OPEN_ONLY, OPEN_ONLY_DETACH_PROCESS
}

enum class FileType {
    IMAGE, VIDEO, GIF, PDF, UNDEFINED
}

const val PDF_PREVIEW_FOLDER_NAME = "pdf_preview"

class PreviewFile(val fileType: FileType, val file: Path) {
    companion object PreviewCompanion {
        fun createPair(file: Path): PreviewFile? {
            return when {
                isImage(file) -> PreviewFile(FileType.IMAGE, file)
                isVideo(file) -> PreviewFile(FileType.VIDEO, file)
                isGif(file) -> PreviewFile(FileType.GIF, file)
                isPDF(file) -> {
                    Log.d("TAG", "createPair isPDF: $file")
                    getPdfPreview(file)
                }
                else -> null
            }
        }

        private fun getPdfPreview(file: Path): PreviewFile {
            val id = computeId(file)
            val savedPreviews = getSavedPdfPreviews()
            return if (savedPreviews?.contains(id) == true){
                PreviewFile(FileType.PDF, getPdfPreviewByID(id))
            }
            else PreviewFile(FileType.PDF, file)
        }
    }
}

private val acceptedImageExt = listOf(".jpg", ".jpeg", ".png")
private val acceptedVideoExt = listOf(".mp4", ".avi", ".mov", ".wmv", ".flv")
private val acceptedEditOnlyExt = arrayListOf(".txt", ".doc", ".docx", ".odt", "ods")
    .also { it.addAll(acceptedImageExt) }
private val acceptedReadAndEditExt = listOf(".pdf", ".md")

fun provideIconImage(file: Path): PreviewFile? =
    providePreview(file) //todo downscale to, say, 128x128

//might be a temporary file
fun providePreview(file: Path): PreviewFile? {
    return PreviewFile.createPair(file)
}


fun isImage(filePath: Path): Boolean {
    val extension = extension(filePath)
    return acceptedImageExt.contains(extension)
}
fun isVideo(filePath: Path): Boolean {
    val extension = extension(filePath)
    return acceptedVideoExt.contains(extension)
}
fun isGif(filePath: Path): Boolean {
    return extension(filePath) == ".gif"
}

fun isPDF(filePath: Path): Boolean {
    return extension(filePath) == ".pdf"
}

fun getPdfPreviewsFolder(): File =
    File("${App.instance.cacheDir}/$PDF_PREVIEW_FOLDER_NAME")

fun getPdfPreviewByID(id: Long): Path {
    val pathName = "${App.instance.cacheDir}/$PDF_PREVIEW_FOLDER_NAME/$id.png"
    Log.d("TAG", "getPdfPreview: $pathName")
    return File(pathName).toPath()
}

fun getSavedPdfPreviews(): List<Long?>? =
    getPdfPreviewsFolder()
        .listFiles()
        ?.map {
            it.nameWithoutExtension.toLongOrNull()
        }

fun createPdfPreview(filePath: Path, context: Context? = null): Bitmap {
    val pageNumber = 0
    val finalContext = context ?: App.instance

    val pdfiumCore = PdfiumCore(finalContext)
    val fd: ParcelFileDescriptor =
        finalContext.contentResolver.openFileDescriptor(Uri.fromFile(filePath.toFile()), "r")!!

    val pdfDocument: PdfDocument = pdfiumCore.newDocument(fd)
    pdfiumCore.openPage(pdfDocument, pageNumber)

    val width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber)
    val height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber)
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height)
    pdfiumCore.closeDocument(pdfDocument)
    return bmp
}

fun getFileActionType(filePath: Path): FileActionType{
    return when(extension(filePath)){
        in acceptedEditOnlyExt -> FileActionType.EDIT_AS_OPEN
        in acceptedReadAndEditExt -> FileActionType.EDIT_AND_OPEN
        in acceptedVideoExt -> FileActionType.OPEN_ONLY_DETACH_PROCESS
        else -> FileActionType.OPEN_ONLY
    }
}

fun isHidden(path: Path): Boolean =
    path.fileName.toString().startsWith('.')

fun folderExists(path: Path): Boolean =
    Files.exists(path) || Files.isDirectory(path)

fun listDevices(): List<Path> =
    App.instance.getExternalFilesDirs(null)
        .toList()
        .filterNotNull()
        .map {
            it.toPath().toRealPath()
                .takeWhile { part ->
                    part != ANDROID_DIRECTORY
                }
                .fold(ROOT_PATH) { parent, child ->
                    parent.resolve(child)
                }
        }

fun findLongestCommonPrefix(paths: List<Path>): Path {
    if (paths.isEmpty()) {
        throw IllegalArgumentException("Can't search for common prefix among empty collection")
    }

    if (paths.size == 1) {
        return paths.first()
    }

    fun tailrec(_prefix: Path, paths: List<Path>): Pair<Path, List<Path>> {
        val grouped = paths.groupBy { it.getName(0) }
        if (grouped.size > 1) {
            return _prefix to paths
        }

        val prefix = _prefix.resolve(grouped.keys.first())
        val shortened = grouped.values.first()
            .map { prefix.relativize(it) }

        return tailrec(prefix, shortened)
    }

    return tailrec(ROOT_PATH, paths).first
}

fun getFileSizeMB(path: Path): Int =
    (path.toFile().length() / (1024 * 1024)).toInt()

fun extension(path: Path): String {
    return ".${path.extension.lowercase()}"
}

fun reifySorting(sorting: Sorting): Comparator<Path>? =
    when (sorting) {
        Sorting.NAME -> compareBy { it.fileName }
        Sorting.SIZE -> compareBy { Files.size(it) }
        Sorting.TYPE -> compareBy { it.fileName.extension }
        Sorting.LAST_MODIFIED -> compareBy { Files.getLastModifiedTime(it) }
        Sorting.DEFAULT -> null
    }