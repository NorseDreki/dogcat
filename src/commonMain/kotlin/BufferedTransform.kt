import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

//TODO would not drain until new tag is met (thus not producing intermediate results)
fun <T> Flow<T>.bufferedTransform(
    shouldDrain: (List<T>, T) -> Boolean,
    transform: (List<T>, T) -> T
): Flow<T> = flow {
    val storage = mutableListOf<T>()

    collect { item ->
        val willDrain = shouldDrain(storage, item)
        if (willDrain) {
            storage.onEach { emit(it) }
            storage.clear()
        }

        val newItem = transform(storage, item)
        storage.add(newItem)
    }
}
