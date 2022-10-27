package space.taran.arknavigator.mvp.model.repo.scores

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.utils.Score

interface ScoreStorage {

    fun contains(id: ResourceId): Boolean

    fun setScore(id: ResourceId, score: Score)

    fun getScore(id: ResourceId): Score?

    fun countScores(): Int

    suspend fun persist()
}
