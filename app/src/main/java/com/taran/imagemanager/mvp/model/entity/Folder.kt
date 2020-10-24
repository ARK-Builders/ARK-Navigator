package com.taran.imagemanager.mvp.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    override var name: String,
    override var path: String,
    var tags: String? = null
): IFile