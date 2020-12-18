package com.taran.imagemanager.mvp.model.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Image (
    var id: Long = 0,
    override var name: String,
    var path: String,
    var tags: String = "",
    var hash: String? = null
): IFile, Parcelable