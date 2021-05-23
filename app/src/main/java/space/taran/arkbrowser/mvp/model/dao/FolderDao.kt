package space.taran.arkbrowser.mvp.model.dao

import androidx.room.*

data class RootWithFavorites(
    @Embedded
    val root: Root,
    @Relation(
        parentColumn = "path",
        entityColumn = "root")
    val favorites: List<Favorite>)

@Dao
interface FolderDao {
    @Insert
    suspend fun insert(root: Root)

    @Insert
    suspend fun insert(vararg favorite: Favorite)

    @Transaction
    @Query("SELECT * FROM Root")
    suspend fun query(): List<RootWithFavorites>
    //todo: looks like it will not yield roots without favorites

    //todo adopt "suspend" functions
}