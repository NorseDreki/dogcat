import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.runningFold

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

/*.runningFold(mutableListOf<LogLine>()) { acc, value ->
                if (acc.size > 1) {
                    val last = acc[acc.size - 1]
                    val previous = acc[acc.size - 2]

                    val r = when {
                        (last is Parsed) && (previous is Parsed) && (last.tag.contains(previous.tag)) -> false
                        else -> true
                    }

                    if (r) {
                        acc.clear()
                    }
                }

                val w = when (value) {
                    is Original -> {
                        //acc.add(value)
                        acc
                    }
                    is Parsed -> {

                        when {
                            acc.size == 0 -> {
                                //println("p $value\r")
                                acc.add(value)
                                mutableListOf()
                            }
                            (acc[0] as Parsed).tag.contains(value.tag) -> {
                                println("p $value\r")
                                val n = Parsed(value.level, "", value.owner, value.message)
                                acc.add(n)
                                mutableListOf()
                            }
                            else -> {
                                acc
                            }
                        }
                    }

                }
                //println(w)
                w
            }
            .flatMapConcat { it.asFlow() }*/


fun <T, K> Flow<T>.groupToList(getKey: (T) -> K): Flow<Pair<K, List<T>>> = flow {
    val storage = mutableMapOf<K, MutableList<T>>()
    collect { t -> storage.getOrPut(getKey(t)) { mutableListOf() } += t }
    storage.forEach { (k, ts) -> emit(k to ts) }
}

data class History<T>(val previous: T?, val current: T)

// emits null, History(null,1), History(1,2)...
fun <T> Flow<T>.runningHistory(): Flow<History<T>?> =
    runningFold(
        initial = null as (History<T>?),
        operation = { accumulator, new -> History(accumulator?.current, new) }
    )
