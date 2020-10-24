package com.taran.imagemanager.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(folder: Folder)

    @Query("SELECT * FROM Folder")
    fun getAll(): List<Folder>

    @Query("SELECT * FROM Folder WHERE path = :path LIMIT 1")
    fun findByPath(path: String): Folder?
}