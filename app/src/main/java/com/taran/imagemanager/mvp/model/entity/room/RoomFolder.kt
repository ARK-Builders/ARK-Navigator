package com.taran.imagemanager.mvp.model.entity.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder")
class RoomFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String,
    var path: String,
    var favorite: Boolean = false,
    var processed: Boolean = false,
    var tags: String? = null
)