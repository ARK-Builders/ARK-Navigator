package space.taran.arkbrowser.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import space.taran.arkbrowser.mvp.model.entity.room.RoomFile

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(file: RoomFile): Long

    @Query("SELECT * FROM RoomFile")
    fun getAll(): List<RoomFile>

    @Query("SELECT * FROM RoomFile WHERE id = :id LIMIT 1")
    fun findById(id: Long): RoomFile?

    @Query("SELECT * FROM RoomFile WHERE path = :path LIMIT 1")
    fun findByPath(path: String): RoomFile?

    @Query("SELECT * FROM RoomFile WHERE fav = 1")
    fun getAllFav(): List<RoomFile>

    @Query("DELETE FROM RoomFile WHERE id = :id")
    fun deleteById(id: Long)
}