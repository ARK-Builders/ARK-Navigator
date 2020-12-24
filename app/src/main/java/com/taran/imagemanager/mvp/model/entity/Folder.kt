package com.taran.imagemanager.mvp.model.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.taran.imagemanager.utils.TEXT_STORAGE_NAME
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Folder(
    var id: Long = 0,
    override var name: String,
    var path: String,
    var favorite: Boolean = false,
    var tags: String = "",
    var synchronized: Boolean = false,
    var lastModified: Long? = null
): IFile, Parcelable {
    val storagePath: String
        get() = "$path/$TEXT_STORAGE_NAME"
}