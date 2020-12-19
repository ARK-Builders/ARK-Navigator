package com.taran.imagemanager.mvp.model.repo

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.IFile
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.ui.file.FileProvider
import com.taran.imagemanager.utils.checkInternalStorage
import com.taran.imagemanager.utils.getHash
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

class FilesRepo(val fileProvider: FileProvider) {

    fun getImagesInFolder(path: String): MutableList<Image> {
        val directory = File(path)
        val inputFiles = directory.listFiles()

        return filterImages(inputFiles).toMutableList()
    }

    fun getFilesInFolder(path: String): MutableList<IFile> {
        val directory = File(path)
        val inputFiles = directory.listFiles()

        val folders = filterFolders(inputFiles)
        val images = filterImages(inputFiles)

        val files = mutableListOf<IFile>()
        files.addAll(folders)
        files.addAll(images)

        return files
    }

    fun getHashSingle(path: String) = Single.create<String> { emitter ->
        emitter.onSuccess(getHash(path))
    }.subscribeOn(Schedulers.io())

    fun mkFile(path: String) = Single.create<Boolean> { emitter ->
        if (fileProvider.isBaseFolder(path)) {
            if (fileProvider.canWrite(path)) {
                emitter.onSuccess(fileProvider.mkFile(path))
            } else {
                fileProvider.deleteDuplicateCardUris(path)
                emitter.onSuccess(false)
            }
        } else
            emitter.onSuccess(fileProvider.mkFile(path))
    }.subscribeOn(Schedulers.io())

    fun writeToFile(path: String, images: List<Image>, override: Boolean) =
        Single.create<Boolean> { emitter ->

            synchronized(this) {
                val data = fileProvider.readFromFile(path)
                val map = mapFromFile(data)

                if (override)
                    images.forEach { image ->
                        map[image.hash!!] = image.tags
                    }
                else
                    images.forEach { image ->
                        if (map[image.hash!!] == null)
                            map[image.hash!!] = image.tags
                    }


                val builder = StringBuilder()
                for ((k, v) in map) {
                    builder.append("\"$k\"=\"$v\"\n")
                }

                emitter.onSuccess(fileProvider.writeToFile(path, builder.toString()))
            }

        }.subscribeOn(Schedulers.io())

    fun getExtSdCards(): List<Folder> {
        return fileProvider.getExtSdCards().map { checkInternalStorage(it) }
    }

    fun getImagesFromGallery(): List<Image> {
        return fileProvider.getImagesFromGallery()
    }

    private fun filterImages(files: Array<File>?): List<Image> {
        return files?.filter { file ->
            val fp = file.absolutePath
            fp.endsWith(".jpg") || fp.endsWith(".png") || fp.endsWith(".jpeg")
        }?.map { file ->
            Image(name = file.name, path = file.absolutePath)
        } ?: listOf()
    }

    private fun filterFolders(files: Array<File>?): List<Folder> {
        return files?.filter { file ->
            file.isDirectory
        }?.map { file ->
            Folder(name = file.name, path = file.absolutePath)
        } ?: listOf()
    }

    private fun mapFromFile(data: String): HashMap<String, String> {
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
}