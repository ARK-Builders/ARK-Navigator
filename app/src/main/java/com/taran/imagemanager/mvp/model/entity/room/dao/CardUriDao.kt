package com.taran.imagemanager.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taran.imagemanager.mvp.model.entity.room.CardUri

@Dao
interface CardUriDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(cardUri: CardUri): Long

    @Query("SELECT * FROM CardUri")
    fun getAll(): List<CardUri>
}