package space.taran.arknavigator.mvp.model.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction

data class RootWithFavorites(
    @Embedded
    val root: Root,
    @Relation(
        parentColumn = "path",
        entityColumn = "root"
    )
    val favorites: List<Favorite>
)

@Dao
interface FolderDao {
    @Insert
    suspend fun insert(root: Root)

    @Insert
    suspend fun insert(vararg favorite: Favorite)

    @Transaction
    @Query("SELECT * FROM Root")
    suspend fun query(): List<RootWithFavorites>
}
