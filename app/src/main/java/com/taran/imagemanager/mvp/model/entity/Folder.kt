package com.taran.imagemanager.mvp.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder")
class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String,
    var path: String,
    var tags: String? = null
): IFile