package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class RoomFile (
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var name: String,
    var path: String,
    var type: String,
    var rootId: Long? = null,
    var tags: String = "",
    var hash: String? = null,
    var isFolder: Boolean,
    var fav: Boolean
)