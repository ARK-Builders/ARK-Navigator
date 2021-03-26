package space.taran.arkbrowser.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import space.taran.arkbrowser.mvp.model.entity.room.RoomRoot

@Dao
interface RootDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(root: RoomRoot): Long

    @Query("SELECT * FROM RoomRoot")
    fun getAll(): List<RoomRoot>

    @Query("DELETE FROM RoomRoot WHERE id = :id")
    fun deleteById(id: Long)
}