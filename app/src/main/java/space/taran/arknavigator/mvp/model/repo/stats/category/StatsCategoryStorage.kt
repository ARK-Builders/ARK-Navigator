package space.taran.arknavigator.mvp.model.repo.stats.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import space.taran.arklib.arkFolder
import space.taran.arklib.arkStats
import space.taran.arklib.domain.stats.StatsEvent
import timber.log.Timber
import java.nio.file.Path

private const val FLUSH_INTERVAL = 10_000L

abstract class StatsCategoryStorage<out T>(
    val root: Path,
    val scope: CoroutineScope
) {
    abstract val fileName: String
    private val flushFlow = MutableSharedFlow<Unit>().also { flow ->
        flow.debounce(FLUSH_INTERVAL).onEach {
            // There may be an exception after root is removed
            try {
                flush()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }.launchIn(scope)
    }

    abstract suspend fun init()

    abstract fun handleEvent(event: StatsEvent)
    abstract fun provideData(): T
    protected abstract fun flush()

    fun locateStorage() = root.arkFolder().arkStats().resolve(fileName)

    protected fun requestFlush() = scope.launch {
        flushFlow.emit(Unit)
    }
}
