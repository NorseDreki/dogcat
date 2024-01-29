import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.TimeSource

//TODO would not drain until new tag is met (thus not producing intermediate results)
fun <T> Flow<T>.bufferedTransform(
    shouldDrain: (List<T>, T) -> Boolean,
    transform: (List<T>, T) -> T
): Flow<T> = flow {
    val storage = mutableListOf<T>()

    collect { item ->
        val willDrain = shouldDrain(storage, item)
        if (willDrain) {
            //storage.onEach { emit(it) }
            storage.clear()
        }

        val newItem = transform(storage, item)
        storage.add(newItem)
        emit(newItem)
    }
}

fun <T> Flow<T>.debounceRepetitiveKeys(debounceTime: Duration): Flow<T> {
    return flow {
        var lastKey: T? = null
        var lastEmissionTime = TimeSource.Monotonic.markNow()

        collect { key ->
            val currentTime = TimeSource.Monotonic.markNow()
            if (key != lastKey || currentTime.elapsedNow() - lastEmissionTime.elapsedNow() >= debounceTime) {
                emit(key)
                lastKey = key
                lastEmissionTime = currentTime
            }
        }
    }
}

fun <T> Flow<T>.windowed(time: Duration): Flow<List<T>> = flow {
    val window = mutableListOf<T>()
    val startTime = TimeSource.Monotonic.markNow()
    collect { value ->
        window.add(value)
        if (TimeSource.Monotonic.markNow() - startTime >= time) {
            emit(window.toList())
            window.clear()
        }
    }
    if (window.isNotEmpty()) {
        emit(window.toList())
    }
}