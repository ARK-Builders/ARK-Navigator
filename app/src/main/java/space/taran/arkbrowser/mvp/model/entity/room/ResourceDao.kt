package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.*
import space.taran.arkbrowser.utils.StringPath

@Dao
interface ResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resources: List<Resource>)

    @Query("DELETE FROM Resource where path in (:paths)")
    suspend fun deletePaths(paths: List<StringPath>)

    @Query("SELECT * FROM Resource")
    suspend fun query(): List<Resource>
}