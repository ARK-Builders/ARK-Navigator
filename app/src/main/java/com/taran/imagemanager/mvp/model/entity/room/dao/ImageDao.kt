package com.taran.imagemanager.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(image: Image)

    @Query("SELECT * FROM Image")
    fun getAll(): List<Image>

    @Query("SELECT * FROM Image WHERE id = :id LIMIT 1")
    fun findById(id: Long): Image?

    @Query("SELECT * FROM Image WHERE path = :path LIMIT 1")
    fun findByPath(path: String): Image?
}