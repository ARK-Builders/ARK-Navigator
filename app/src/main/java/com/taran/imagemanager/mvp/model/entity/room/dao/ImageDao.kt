package space.taran.arkbrowser.mvp.model.entity.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import space.taran.arkbrowser.mvp.model.entity.Folder
import space.taran.arkbrowser.mvp.model.entity.Image
import space.taran.arkbrowser.mvp.model.entity.room.RoomImage

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(image: RoomImage): Long

    @Query("SELECT * FROM image")
    fun getAll(): List<RoomImage>

    @Query("SELECT * FROM image WHERE id = :id LIMIT 1")
    fun findById(id: Long): RoomImage?

    @Query("SELECT * FROM image WHERE path = :path LIMIT 1")
    fun findByPath(path: String): RoomImage?

    @Query("UPDATE image SET tags = :tags WHERE id = :id")
    fun updateTags(id: Long, tags: String)
}