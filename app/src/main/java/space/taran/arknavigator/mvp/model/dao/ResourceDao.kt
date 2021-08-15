package space.taran.arknavigator.mvp.model.dao

import androidx.room.*
import space.taran.arknavigator.utils.StringPath

@Dao
interface ResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resources: List<Resource>)

    @Query("DELETE FROM Resource where path in (:paths)")
    suspend fun deletePaths(paths: List<StringPath>)

    @Query("SELECT * FROM Resource where root = :root")
    suspend fun query(root: StringPath): List<Resource>
    //todo: can be optimized with `root in (:roots)`
}