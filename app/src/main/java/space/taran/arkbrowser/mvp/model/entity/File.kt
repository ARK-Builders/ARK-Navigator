package space.taran.arkbrowser.mvp.model.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import space.taran.arkbrowser.utils.Constants.Companion.NO_TAGS
import space.taran.arkbrowser.utils.Tags

@Parcelize
data class File (
    var id: Long = 0,
    var name: String,
    var path: String,
    var type: String,
    var size: Long,
    var lastModified: Long,
    var rootId: Long? = null,
    var tags: Tags = NO_TAGS,
    var hash: String? = null,
    var isFolder: Boolean,
    var fav: Boolean = false,
    var synchronized: Boolean = false
): Parcelable {
    fun isImage() = type == "jpg" || type == "png" || type == "jpeg"
}