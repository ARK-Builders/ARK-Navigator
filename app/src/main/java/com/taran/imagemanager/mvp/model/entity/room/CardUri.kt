package com.taran.imagemanager.mvp.model.entity.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class CardUri(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    var uri: String? = null
)