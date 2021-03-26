package space.taran.arkbrowser.utils

import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.room.RoomFile
import space.taran.arkbrowser.mvp.model.entity.room.RoomRoot

fun mapFileFromRoom(roomFile: RoomFile) = File(
    roomFile.id,
    roomFile.name,
    roomFile.path,
    roomFile.type,
    roomFile.rootId,
    roomFile.tags,
    roomFile.hash,
    roomFile.isFolder,
    roomFile.fav
)

fun mapFileToRoom(file: File) = RoomFile(
    file.id,
    file.name,
    file.path,
    file.type,
    file.rootId,
    file.tags,
    file.hash,
    file.isFolder,
    file.fav
)

fun mapRootFromRoom(roomRoot: RoomRoot) = Root(
    roomRoot.id,
    roomRoot.name,
    roomRoot.parentUri,
    roomRoot.storageUri,
    false,
    storageLastModified = roomRoot.lastModified
)

fun mapRootToRoom(root: Root) = RoomRoot(
    root.id,
    root.name,
    root.parentPath,
    root.storagePath,
    root.storageLastModified
)