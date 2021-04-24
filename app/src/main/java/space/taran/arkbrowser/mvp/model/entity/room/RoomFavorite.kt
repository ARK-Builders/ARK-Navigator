package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class RoomFavorite (
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var path: String
)