package space.taran.arkbrowser.mvp.model.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class File (
    var id: Long = 0,
    var name: String,
    var path: String,
    var type: String,
    var rootId: Long? = null,
    var tags: String = "",
    var hash: String? = null,
    var isFolder: Boolean,
    var fav: Boolean = false,
    var synchronized: Boolean = false
): Parcelable {
    fun isImage() = type == "jpg" || type == "png" || type == "jpeg"
}