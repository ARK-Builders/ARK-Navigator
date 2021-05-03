package space.taran.arkbrowser.mvp.model.entity.room

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
    fun insert(root: Root)

    @Insert
    fun insert(vararg favorite: Favorite)

    @Transaction
    @Query("SELECT * FROM Root")
    fun query(): List<RootWithFavorites>
    //todo: looks like it will not yield roots without favorites

    //todo adopt "suspend" functions
}