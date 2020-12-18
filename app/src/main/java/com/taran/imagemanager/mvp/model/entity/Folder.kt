package com.taran.imagemanager.mvp.model.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Folder(
    var id: Long = 0,
    override var name: String,
    var path: String,
    var favorite: Boolean = false,
    var processed: Boolean = false,
    var tags: String = ""
): IFile, Parcelable