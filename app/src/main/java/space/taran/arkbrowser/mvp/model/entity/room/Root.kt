package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import space.taran.arkbrowser.utils.StringPath

@Entity
data class Root(
    @PrimaryKey(autoGenerate = false)
    val path: StringPath)