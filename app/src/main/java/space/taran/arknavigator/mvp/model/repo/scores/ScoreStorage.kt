package space.taran.arknavigator.mvp.model.repo.scores

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arknavigator.utils.Score

interface ScoreStorage {

    fun contains(id: ResourceId): Boolean

    fun setScore(id: ResourceId, score: Score)

    fun getScore(id: ResourceId): Score

    suspend fun resetScores(resources: List<ResourceMeta>)

    suspend fun persist()
}
