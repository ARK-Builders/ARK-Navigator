package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.room.RoomRoot

import space.taran.arkbrowser.utils.getHash
import space.taran.arkbrowser.utils.mapFileToRoom
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.ReplaySubject

class SynchronizeRepo(val roomRepo: RoomRepo, val filesRepo: FilesRepo) {
    private val subjectsMap = hashMapOf<Long, ReplaySubject<File>>()
    val roots = mutableListOf<Root>()

    fun getSyncObservable(root: Root) = subjectsMap[root.id]

    fun synchronizeRoot(root: Root) {
        if (roots.find { it.id == root.id } == null) {
            roots.add(root)
            subjectsMap[root.id] = ReplaySubject.create()
            if (filesRepo.getRootLastModified(root) == root.storageLastModified)
                synchronizeWithDB(root, subjectsMap[root.id]!!).subscribe()
            else
                synchronizeWithFile(root, subjectsMap[root.id]!!).subscribe()
        }
    }

    private fun synchronizeWithDB(root: Root, subject: ReplaySubject<File>) =
        Completable.create { emitter ->
            root.files.addAll(filesRepo.getAllFiles(root.parentPath))
            root.files.filter { file -> file.isImage() }.forEach { image ->
                val roomImage = roomRepo.database.fileDao().findByPath(image.path)
                if (roomImage == null) {
                    image.hash = getHash(filesRepo.fileDataSource.getBytes(image.path))
                    roomRepo.database.fileDao().insert(mapFileToRoom(image))
                } else {
                    image.id = roomImage.id
                    image.hash = roomImage.hash
                    image.tags = roomImage.tags
                }
                image.rootId = root.id
                image.synchronized = true
                subject.onNext(image)
            }

            root.synchronized = true

            emitter.onComplete()
        }.subscribeOn(Schedulers.io())

    private fun synchronizeWithFile(root: Root, subject: ReplaySubject<File>) =
        Completable.create { emitter ->
            root.files.addAll(filesRepo.getAllFiles(root.parentPath))
            val fileMap = filesRepo.readFromStorage(root.storagePath)

            root.files.filter { file -> file.isImage() }.forEach { image ->
                val roomImage = roomRepo.database.fileDao().findByPath(image.path)
                if (roomImage == null) {
                    image.hash = getHash(filesRepo.fileDataSource.getBytes(image.path))
                } else {
                    image.id = roomImage.id
                    image.hash = roomImage.hash
                }

                val stored = fileMap.get(image.hash)
                if (!stored.isNullOrEmpty()) {
                    image.tags = image.tags.union(stored)
                }

                image.rootId = root.id
                image.synchronized = true
                roomRepo.database.fileDao().insert(mapFileToRoom(image))

                subject.onNext(image)
            }

            filesRepo.writeToStorage(root.storagePath, root.files)
            root.storageLastModified = filesRepo.getRootLastModified(root)
            roomRepo.database.rootDao().insert(
                RoomRoot(
                    root.id,
                    root.name,
                    root.parentPath,
                    root.storagePath,
                    root.storageLastModified
                )
            )

            root.synchronized = true
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())

    fun getRootForId(id: Long): Root? {
        roots.forEach { root ->
            if (root.id == id)
                return root
        }
        return null
    }

    fun getRootByPath(path: String): Root? {
        roots.forEach { root ->
            if (path.contains(root.parentPath))
                return root
        }

        return null
    }
}