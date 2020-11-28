package com.taran.imagemanager.mvp.model.entity.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image")
class RoomImage (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String,
    var path: String,
    var tags: String? = null,
    var hash: String? = null
)