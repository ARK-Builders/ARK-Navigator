package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.ui.file.DocumentProvider
import space.taran.arkbrowser.ui.file.FileProvider
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers

class FilesRepo(val fileProvider: FileProvider, val documentProvider: DocumentProvider) {

    companion object {
        private const val TEXT_MIME_TYPE = "text/plain"
        const val TEXT_STORAGE_NAME = ".ark-tags.txt"
        private const val DUMMY_FILE_NAME = "dummy.txt"
    }

    fun getAllFiles(path: String): List<File> {
        val files = mutableListOf<File>()
        fileProvider.list(path).forEach { file ->
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
            builder.append("\"${file.hash}\"=\"${file.tags}\"\n")
        }

        if (!fileProvider.write(path, builder.toString()))
            documentProvider.write(path, builder.toString())
    }

    fun writeToStorageAsync(path: String, files: List<File>) = Completable.create { emitter ->
        writeToStorage(path, files)
        emitter.onComplete()
    }.subscribeOn(Schedulers.io())

    fun readFromStorage(path: String): HashMap<String, String> {
        val data = fileProvider.read(path)

        val lines = data.split("\n")
        val map = HashMap<String, String>()

        lines.forEach { line ->
            if (line.isNotEmpty()) {
                val values = line.split("=")
                val hash = values[0].replace("\"", "")
                val tags = values[1].replace("\"", "")
                map[hash] = tags
            }
        }

        return map
    }

    fun getRootLastModified(root: Root): Long {
        return fileProvider.getLastModified(root.storagePath)
    }

    fun createStorage(parentPath: String): String? {
        if (!fileProvider.mk("$parentPath/$DUMMY_FILE_NAME")) {
            if (!documentProvider.mk(parentPath, DUMMY_FILE_NAME, TEXT_MIME_TYPE))
                return null
            else
                documentProvider.remove("$parentPath/$DUMMY_FILE_NAME")
        } else
            fileProvider.remove("$parentPath/$DUMMY_FILE_NAME")

        return if (fileProvider.mk("$parentPath/$TEXT_STORAGE_NAME"))
            "$parentPath/$TEXT_STORAGE_NAME"
        else {
            if (documentProvider.mk(parentPath, TEXT_STORAGE_NAME, TEXT_MIME_TYPE))
                "$parentPath/$TEXT_STORAGE_NAME"
            else
                null
        }
    }
}