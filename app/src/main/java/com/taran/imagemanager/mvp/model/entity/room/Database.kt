package com.taran.imagemanager.mvp.model.entity.room

import androidx.room.RoomDatabase
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.entity.room.dao.FolderDao
import com.taran.imagemanager.mvp.model.entity.room.dao.ImageDao

@androidx.room.Database(
    entities = [
        RoomFolder::class,
        RoomImage::class
    ],
    version = 7,
    exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun imageDao(): ImageDao

    companion object {
        const val DB_NAME = "ImageManager.db"
    }
}