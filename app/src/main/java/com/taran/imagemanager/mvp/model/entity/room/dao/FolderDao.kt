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

    @Query("SELECT * FROM folder")
    fun getAll(): List<Folder>

    @Query("SELECT * FROM folder WHERE path = :path LIMIT 1")
    fun findByPath(path: String): Folder?

    @Query("SELECT * FROM folder WHERE favorite = 1")
    fun getAllFavorite(): List<Folder>
}