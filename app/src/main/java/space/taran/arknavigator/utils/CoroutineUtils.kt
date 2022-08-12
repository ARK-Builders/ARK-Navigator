package space.taran.arknavigator.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

suspend fun <T> withContextAndLock(
    context: CoroutineContext,
    mutex: Mutex,
    block: suspend CoroutineScope.() -> T
): T = withContext(context) {
    mutex.withLock {
        block()
    }
}

fun Mutex.tryUnlock() {
    if (isLocked)
        unlock()
}

fun tickerFlow(
    delayMillis: Long,
    initialDelayMillis: Long = 0
): Flow<Unit> = flow {
    delay(initialDelayMillis)
    while (currentCoroutineContext().isActive) {
        emit(Unit)
        delay(delayMillis)
    }
}
