package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.Resource
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.utils.*
import space.taran.arkbrowser.utils.Converters.Companion.stringFromTags
import space.taran.arkbrowser.utils.Converters.Companion.tagsFromString
import java.io.File

class ResourcesRepo(val roomRepo: RoomRepo, val tagsStorage: TagsStorage) {
    companion object {
        const val TAGS_STORAGE_NAME = ".ark-tags"
        const val KEY_VALUE_SEPARATOR = ':'
        const val STORAGE_VERSION = 1

        private const val TEXT_MIME_TYPE = "text/plain"
        private const val DUMMY_FILE_NAME = ".dummy"
        private const val KEY_VALUE_STORAGE_VERSION = "version"
    }

    fun retrieveResources(folder: File): Set<Resource> {
        val (directories, files) = listChildren(folder)
            .partition { it.isDirectory }

        val resources = files.map {
            Resource(
                name = it.name,
                type = it.extension,
                file = it,
                size = it.length(),
                lastModified = it.lastModified()
            )
        }

        return resources.toSet() + directories.flatMap {
            retrieveResources(it)
        }
    }

    fun writeToStorage(storage: File, resources: Set<Resource>) {
        val builder = StringBuilder()
        builder.append("$KEY_VALUE_STORAGE_VERSION$KEY_VALUE_SEPARATOR$STORAGE_VERSION\n")

        resources.forEach { resource ->
            val string = stringFromTags(resource.tags)
            builder.append("${resource.hash}${KEY_VALUE_SEPARATOR}${string}\n")
        }

        if (!write(storage, builder.toString())) {
            documentDataSource.write(storage, builder.toString())
        }
    }

    fun writeToStorageAsync(storage: File, resources: Set<Resource>) = Completable.create { emitter ->
        writeToStorage(storage, resources)
        emitter.onComplete()
    }.subscribeOn(Schedulers.io())

    fun readStorageVersion(storage: File): Int {
        val line = readFirstLine(storage)
        val fields = line.split(KEY_VALUE_SEPARATOR)
        return fields[1].toInt()
    }

    fun readFromStorage(tagsStorage: File): Map<String, Tags> =
        read(tagsStorage)
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

    fun getStorageLastModified(root: Root): Long {
        return root.storage.lastModified()
    }

    fun createStorage(parent: File): File? {
        val dummyFile = File(parent, DUMMY_FILE_NAME)

        if (!checkOrCreate(dummyFile)) {
            if (!documentDataSource.createDocument(parent, DUMMY_FILE_NAME, TEXT_MIME_TYPE)) {
                return null
            } else {
                documentDataSource.remove(dummyFile)
            }
        } else {
            remove(dummyFile)
        }

        val storageFile = File(parent, TAGS_STORAGE_NAME)

        return if (checkOrCreate(storageFile) ||
                   documentDataSource.createDocument(parent, TAGS_STORAGE_NAME, TEXT_MIME_TYPE)) {
            storageFile
        } else {
            null
        }
    }




    private fun synchronize(root: Root) {
        val hashToTags = resourcesRepo.readFromStorage(root.storage)
        root.resources = resourcesRepo.retrieveResources(root.folder)

        root.resources!!.forEach { resource ->
            resource.rootId = root.id

            val known = roomRepo.database.resourceDao().findByPath(resource.file.path)
            if (known == null) {
                resource.hash = computeHash(readBytes(resource.file))
            } else {
                resource.id = known.id
                resource.hash = known.hash
            }

            val stored = hashToTags[resource.hash]
            if (stored != null) {
                resource.tags = resource.tags.union(stored)
            }

            roomRepo.database.resourceDao().insert(mapResourceToRoom(resource))
            subject.onNext(resource)
        }

        resourcesRepo.writeToStorage(root.storage, root.resources!!)
        roomRepo.database.rootDao().insert(mapRootToRoom(root))

        emitter.onComplete()
    }
}