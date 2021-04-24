package space.taran.arkbrowser.mvp.model.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import space.taran.arkbrowser.mvp.model.entity.room.RoomRoot
import java.io.File

typealias RootId = Long

@Parcelize
data class Root (
    val id: RootId = 0,
    val folder: File //todo: maybe Uri is necessary
): Parcelable {
    companion object {
        fun fromRoom(root: RoomRoot): Root =
            Root(root.id,
                File(root.path))    //todo or Uri.parse()
    }
}