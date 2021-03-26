package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class RoomRoot (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var name: String,
    var parentUri: String,
    val storageUri: String,
    var lastModified: Long? = null
)