package flow

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class ClosedException(val owner: FlowCollector<*>) :
    Exception("Flow was aborted, no more elements needed")

internal fun ClosedException.checkOwnership(owner: FlowCollector<*>) {
    if (this.owner !== owner) throw this
}

fun <T> Flow<T>.takeUntil(notifier: Flow<Any?>): Flow<T> = flow {
    try {
        coroutineScope {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                notifier.take(1).collect()
                throw ClosedException(this@flow)
            }

            collect { emit(it) }
            job.cancel()
        }
    } catch (e: ClosedException) {
        e.checkOwnership(this@flow)
    }
}
