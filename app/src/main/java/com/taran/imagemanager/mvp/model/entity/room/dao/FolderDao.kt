package com.taran.imagemanager.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.entity.room.RoomFolder

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(folder: RoomFolder)

    @Query("SELECT * FROM folder")
    fun getAll(): List<RoomFolder>

    @Query("SELECT * FROM folder WHERE path = :path LIMIT 1")
    fun findByPath(path: String): RoomFolder?

    @Query("SELECT * FROM folder WHERE favorite = 1")
    fun getAllFavorite(): List<RoomFolder>
}