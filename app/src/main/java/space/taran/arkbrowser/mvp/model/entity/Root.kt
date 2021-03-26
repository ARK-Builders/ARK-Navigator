package space.taran.arkbrowser.mvp.model.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Root (
    var id: Long = 0,
    var name: String,
    var parentPath: String,
    val storagePath: String,
    var synchronized: Boolean = false,
    val files: MutableList<File> = mutableListOf(),
    var storageLastModified: Long? = null
): Parcelable