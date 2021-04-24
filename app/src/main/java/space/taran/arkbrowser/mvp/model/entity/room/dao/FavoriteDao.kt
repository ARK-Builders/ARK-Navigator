package space.taran.arkbrowser.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import space.taran.arkbrowser.mvp.model.entity.room.RoomFavorite

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(file: RoomFavorite): Long

    @Query("SELECT * FROM RoomFavorite")
    fun getAll(): List<RoomFavorite>

    @Query("DELETE FROM RoomFavorite WHERE id = :id")
    fun deleteById(id: Long)
}