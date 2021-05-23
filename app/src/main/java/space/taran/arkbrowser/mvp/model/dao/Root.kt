package space.taran.arkbrowser.mvp.model.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import space.taran.arkbrowser.utils.StringPath

@Entity
data class Root(
    @PrimaryKey(autoGenerate = false)
    val path: StringPath)