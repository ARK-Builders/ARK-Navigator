package dev.arkbuilders.navigator.mvp.model.repo.stats

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.arkbuilders.navigator.utils.Tag

class AggregatedStatsStorage(val shards: List<StatsStorage>) : StatsStorage {

    override suspend fun init() = withContext(Dispatchers.IO) {
        shards.map { launch { it.init() } }.joinAll()
    }

    override fun handleEvent(event: StatsEvent) =
        shards.forEach { it.handleEvent(event) }

    override fun statsTagLabeledAmount(): Map<Tag, Int> =
        shards
            .map { it.statsTagLabeledAmount() }
            .fold(mutableMapOf()) { acc, shard ->
                shard.forEach { (tag, amount) ->
                    acc.merge(tag, amount, Int::plus)
                }
                acc
            }

    override fun statsTagQueriedAmount(): Map<Tag, Int> =
        shards
            .map { it.statsTagQueriedAmount() }
            .fold(mutableMapOf()) { acc, shard ->
                shard.forEach { (tag, amount) ->
                    acc.merge(tag, amount, Int::plus)
                }
                acc
            }

    override fun statsTagQueriedTS(): Map<Tag, Long> = shards
        .map { it.statsTagQueriedTS() }
        .fold(mutableMapOf()) { acc, shard ->
            shard.forEach { (tag, shardTS) ->
                acc[tag] = maxOf(acc[tag] ?: -1, shardTS)
            }
            acc
        }

    override fun statsTagLabeledTS(): Map<Tag, Long> = shards
        .map { it.statsTagLabeledTS() }
        .fold(mutableMapOf()) { acc, shard ->
            shard.forEach { (tag, shardTS) ->
                acc[tag] = maxOf(acc[tag] ?: -1, shardTS)
            }
            acc
        }
}
