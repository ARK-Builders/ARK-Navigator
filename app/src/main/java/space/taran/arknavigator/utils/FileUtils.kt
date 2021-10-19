package space.taran.arknavigator.utils

import android.content.Context
import space.taran.arknavigator.ui.App
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import space.taran.arknavigator.ui.fragments.utils.Preview
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.extension
import kotlin.io.path.fileSize

val ROOT_PATH: Path = Paths.get("/")

val ANDROID_DIRECTORY: Path = Paths.get("Android")

typealias Milliseconds = Long
typealias StringPath = String

enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}

enum class FileActionType {
    EDIT_AS_OPEN, EDIT_AND_OPEN, OPEN_ONLY, OPEN_ONLY_DETACH_PROCESS
}

const val PDF_PREVIEW_FOLDER_NAME = "pdf_preview"

private val acceptedImageExt = listOf(".jpg", ".jpeg", ".png")
private val acceptedVideoExt = listOf(".mp4", ".avi", ".mov", ".wmv", ".flv")
private val acceptedEditOnlyExt = arrayListOf(".txt", ".doc", ".docx", ".odt", "ods")
    .also { it.addAll(acceptedImageExt) }
private val acceptedReadAndEditExt = listOf(".pdf", ".md")

fun isImage(filePath: Path): Boolean {
    val extension = extension(filePath)
    return acceptedImageExt.contains(extension)
}

fun isVideo(filePath: Path): Boolean {
    return if (filePath.fileSize() > 0) {
        val extension = extension(filePath)
        acceptedVideoExt.contains(extension)
    } else {
        false
    }
}

fun isPDF(filePath: Path): Boolean {
    return extension(filePath) == ".pdf"
}

fun isFormat(filePath: Path, format: String): Boolean {
    return extension(filePath) == format
}

fun getPdfPreviewsFolder(): File =
    File("${App.instance.cacheDir}/$PDF_PREVIEW_FOLDER_NAME")

fun getPdfPreviewByID(id: Long): Path {
    val pathName = "${App.instance.cacheDir}/$PDF_PREVIEW_FOLDER_NAME/$id.png"
    return File(pathName).toPath()
}

fun getSavedPdfPreviews(): List<Long?>? =
    getPdfPreviewsFolder()
        .listFiles()
        ?.map {
            it.nameWithoutExtension.toLongOrNull()
        }

fun getVideoInfo(filePath: Path): MutableMap<Preview.ExtraInfoTag, String> {
    val retriever = MediaMetadataRetriever()

    retriever.setDataSource(App.instance, Uri.fromFile(filePath.toFile()))
    val timeMillis =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    val width =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toLong() ?: 0L
    val height =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toLong() ?: 0L


    var minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis).toString()
    val minutesPassedInMillis = TimeUnit.MINUTES.toMillis(minutes.toLong()).toString()
    var seconds =
        TimeUnit.MILLISECONDS.toSeconds(timeMillis - minutesPassedInMillis.toLong()).toString()

    if (minutes.length == 1) minutes = "0$minutes"
    if (seconds.length == 1) seconds = "0$seconds"

    val duration = "$minutes:$seconds"
    val resolution = convertToResolution(width, height)

    val mutableMap = mutableMapOf<Preview.ExtraInfoTag, String>()
    mutableMap[Preview.ExtraInfoTag.MEDIA_DURATION] = duration

    if (resolution != null)
        mutableMap[Preview.ExtraInfoTag.MEDIA_RESOLUTION] = resolution

    retriever.release()
    return mutableMap
}

fun convertToResolution(width: Long, height: Long): String? {
    val resolutionPair = listOf(width, height)

    return when {
        resolutionPair.containsAll(listOf(256, 144)) -> "144p"
        resolutionPair.containsAll(listOf(426, 240)) -> "240p"
        resolutionPair.containsAll(listOf(640, 360)) -> "360p"
        resolutionPair.containsAll(listOf(854, 480)) -> "480p"
        resolutionPair.containsAll(listOf(1280, 720)) -> "720p"
        resolutionPair.containsAll(listOf(1920, 1080)) -> "1080p"
        resolutionPair.containsAll(listOf(2560, 1440)) -> "1440p"
        resolutionPair.containsAll(listOf(3840, 2160)) -> "2160p"
        else -> null
    }
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

fun getFileActionType(filePath: Path): FileActionType {
    return when (extension(filePath)) {
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

fun extension(path: Path): String {
    return ".${path.extension.lowercase()}"
}

fun extensionWithoutDot(path: Path): String {
    return path.extension.lowercase()
}

fun reifySorting(sorting: Sorting): Comparator<Path>? =
    when (sorting) {
        Sorting.NAME -> compareBy { it.fileName }
        Sorting.SIZE -> compareBy { Files.size(it) }
        Sorting.TYPE -> compareBy { it.fileName.extension }
        Sorting.LAST_MODIFIED -> compareBy { Files.getLastModifiedTime(it) }
        Sorting.DEFAULT -> null
    }

const val KILOBYTE = 1024
const val MEGABYTE = 1024 * KILOBYTE