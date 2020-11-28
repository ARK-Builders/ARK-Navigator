package com.taran.imagemanager.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.entity.room.RoomImage

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(image: RoomImage)

    @Query("SELECT * FROM image")
    fun getAll(): List<RoomImage>

    @Query("SELECT * FROM image WHERE id = :id LIMIT 1")
    fun findById(id: Long): RoomImage?

    @Query("SELECT * FROM image WHERE path = :path LIMIT 1")
    fun findByPath(path: String): RoomImage?
}