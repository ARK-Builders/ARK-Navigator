package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.*
import space.taran.arkbrowser.utils.StringPath

@Dao
interface ResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(resources: List<Resource>)

    @Query("DELETE FROM Resource where path = :path")
    fun deleteByPath(path: StringPath)

    @Query("SELECT * FROM Resource")
    fun query(): List<Resource>
}