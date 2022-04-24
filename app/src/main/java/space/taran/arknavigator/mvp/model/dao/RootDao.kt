package space.taran.arknavigator.mvp.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RootDao {
    @Insert
    suspend fun insert(root: Root)

    @Transaction
    @Query("SELECT * FROM Root")
    suspend fun query(): List<Root>
}
