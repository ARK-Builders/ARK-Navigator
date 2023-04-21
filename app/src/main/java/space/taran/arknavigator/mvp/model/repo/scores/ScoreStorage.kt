package space.taran.arknavigator.mvp.model.repo.scores

import space.taran.arklib.ResourceId
import space.taran.arknavigator.utils.Score

interface ScoreStorage {

    fun contains(id: ResourceId): Boolean

    fun setScore(id: ResourceId, score: Score)

    fun getScore(id: ResourceId): Score

    suspend fun resetScores(resources: List<ResourceId>)

    suspend fun persist()
}
