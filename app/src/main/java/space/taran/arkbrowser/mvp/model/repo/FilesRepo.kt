package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.ui.file.DocumentDataSource
import space.taran.arkbrowser.ui.file.FileDataSource
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import space.taran.arkbrowser.utils.Converters.Companion.stringFromTags
import space.taran.arkbrowser.utils.Converters.Companion.tagsFromString
import space.taran.arkbrowser.utils.Tags

class FilesRepo(val fileDataSource: FileDataSource, val documentDataSource: DocumentDataSource) {

    companion object {
        const val TEXT_STORAGE_NAME = ".ark-tags.txt"
        const val KEY_VALUE_SEPARATOR = ':'

        private const val TEXT_MIME_TYPE = "text/plain"
        private const val DUMMY_FILE_NAME = "dummy.txt"
    }

    fun getAllFiles(path: String): List<File> {
        val files = mutableListOf<File>()
        fileDataSource.list(path).forEach { file ->
            if (file.isFolder)
                files.addAll(getAllFiles(file.path))
            else
                files.add(file)
        }
        return files
    }

    fun writeToStorage(path: String, files: List<File>) {
        val builder = StringBuilder()

        files.forEach { file ->
            val string = stringFromTags(file.tags)
            builder.append("${file.hash}${KEY_VALUE_SEPARATOR}${string}\n")
        }

        if (!fileDataSource.write(path, builder.toString())) {
            documentDataSource.write(path, builder.toString())
        }
    }

    fun writeToStorageAsync(path: String, files: List<File>) = Completable.create { emitter ->
        writeToStorage(path, files)
        emitter.onComplete()
    }.subscribeOn(Schedulers.io())

    fun readFromStorage(path: String): Map<String, Tags> =
        fileDataSource.read(path)
            .split("\n")
            .filter { line ->
                line.isNotEmpty() && line.contains(KEY_VALUE_SEPARATOR)
            }
            .map { line ->
                val fields = line.split(KEY_VALUE_SEPARATOR)
                val hash = fields[0]
                val tags = tagsFromString(fields[1])

                hash to tags
            }
            .toMap()

    fun getRootLastModified(root: Root): Long {
        return fileDataSource.getLastModified(root.storagePath)
    }

    fun createStorage(parentPath: String): String? {
        if (!fileDataSource.mk("$parentPath/$DUMMY_FILE_NAME")) {
            if (!documentDataSource.mk(parentPath, DUMMY_FILE_NAME, TEXT_MIME_TYPE))
                return null
            else
                documentDataSource.remove("$parentPath/$DUMMY_FILE_NAME")
        } else
            fileDataSource.remove("$parentPath/$DUMMY_FILE_NAME")

        return if (fileDataSource.mk("$parentPath/$TEXT_STORAGE_NAME"))
            "$parentPath/$TEXT_STORAGE_NAME"
        else {
            if (documentDataSource.mk(parentPath, TEXT_STORAGE_NAME, TEXT_MIME_TYPE))
                "$parentPath/$TEXT_STORAGE_NAME"
            else
                null
        }
    }
}