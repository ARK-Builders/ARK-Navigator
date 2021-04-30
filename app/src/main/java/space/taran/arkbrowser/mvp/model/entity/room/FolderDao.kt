package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.*

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(folder: Folder)

    @Delete
    fun delete(folder: Folder)

    @Query("SELECT * FROM Folder")
    fun getAll(): List<Folder>
}