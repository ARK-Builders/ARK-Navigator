package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

import space.taran.arkbrowser.utils.Constants.Companion.NO_TAGS
import space.taran.arkbrowser.utils.Converters
import space.taran.arkbrowser.utils.Tags

@Entity
@TypeConverters(Converters::class)
class RoomFile (
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var name: String,
    var path: String,
    var type: String,
    var rootId: Long? = null,
    var tags: Tags = NO_TAGS,
    var hash: String? = null,
    var isFolder: Boolean,
    var fav: Boolean
)