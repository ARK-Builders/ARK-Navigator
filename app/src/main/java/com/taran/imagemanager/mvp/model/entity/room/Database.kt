package com.taran.imagemanager.mvp.model.entity.room

import androidx.room.RoomDatabase
import com.taran.imagemanager.mvp.model.entity.room.dao.CardUriDao
import com.taran.imagemanager.mvp.model.entity.room.dao.FolderDao
import com.taran.imagemanager.mvp.model.entity.room.dao.ImageDao

@androidx.room.Database(
    entities = [
        RoomFolder::class,
        RoomImage::class,
        CardUri::class
    ],
    version = 8,
    exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun imageDao(): ImageDao
    abstract fun cardUriDao(): CardUriDao
}