package space.taran.arkbrowser.mvp.model.entity.room.dao

import androidx.room.*
import space.taran.arkbrowser.mvp.model.entity.room.Resource
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.utils.StringPath

@Dao
interface ResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(resources: List<Resource>)

    @Delete
    fun deleteByPath(path: StringPath)

    @Insert
    fun update(resource: Resource)

    @Query("SELECT * FROM Resource")
    fun getAll(): List<Resource>
}