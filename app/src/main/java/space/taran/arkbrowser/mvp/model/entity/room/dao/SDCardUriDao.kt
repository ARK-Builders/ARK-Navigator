package space.taran.arkbrowser.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import space.taran.arkbrowser.mvp.model.entity.room.SDCardUri

@Dao
interface SDCardUriDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sdCardUri: SDCardUri): Long

    @Query("SELECT * FROM SDCardUri WHERE path = :path LIMIT 1")
    fun findByPath(path: String): SDCardUri?

    @Query("SELECT * FROM SDCardUri")
    fun getAll(): List<SDCardUri>


}
