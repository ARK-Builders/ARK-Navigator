package space.taran.arkbrowser.mvp.model.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import space.taran.arkbrowser.mvp.model.entity.room.RoomFavorite
import java.io.File

//todo: join with Root class

@Parcelize
data class Favorite (
    val id: Long = 0,
    val file: File     //todo: maybe Uri is necessary
): Parcelable {
    companion object {
        fun fromRoom(favorite: RoomFavorite): Favorite =
            Favorite(
                favorite.id,
                File(favorite.path))   //todo or Uri.parse()
    }
}