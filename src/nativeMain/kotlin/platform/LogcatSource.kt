package platform

import dogcat.LogSource
import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import dogcat.InternalState
import dogcat.LogFilter
import dogcat.LogFilter.ByExcludedTags
import dogcat.LogFilter.ByTime
import dogcat.State
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LogcatSource(
    val state: InternalState
) : LogSource {

    private val scope = CoroutineScope(Dispatchers.IO)

    val v = state.appliedFilters
        .filter { it.containsKey(ByExcludedTags::class) }
        .map { it[ByExcludedTags::class]!! }
        .map {
            val args = mutableListOf<String>()

            if (it.second) {
                (it.first as ByExcludedTags).exclusions.forEach { args.add("$it:S") }
            }

            args
        }
        .onStart { emit(mutableListOf("Looper:S")) }
        .distinctUntilChanged()

    val v1 = state.appliedFilters
        .onEach { println("$it jhagdljahgsdjlashgiu3yei2u3yieuy23iuyh") }
        .filter { it.containsKey(LogFilter.MinLogLevel::class) }
        .map { it[LogFilter.MinLogLevel::class]!! }
        .map {
            val args = mutableListOf<String>()

            if (it.second) {
                val ll = (it.first as LogFilter.MinLogLevel).logLevel
                args.add("*:$ll")
            }

            args
        }
        //.onStart { emit(mutableListOf("*:W")) }
        .distinctUntilChanged()

    val v2 = state.appliedFilters
        .filter { it.containsKey(ByTime::class) }
        .map { it[ByTime::class]!! }
        .map {
            val args = mutableListOf<String>()

            if (it.second) {
                val ll = (it.first as LogFilter.ByTime).l
                args.add("-t \"$ll\"")
            }

            args
        }
        .onStart { emit(mutableListOf("")) }
        .distinctUntilChanged()

    fun lines11(strings: List<String>): Flow<String> {
        return flow {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .args(*strings.toTypedArray())
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader: Reader? = child.getChildStdout()

            while (true) { //EOF??
                //ensureActive() -- call in scope
                val line2 = stdoutReader!!.readLine() ?: break
                emit(line2)
                //if (isActive)
                yield()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun lines(): Flow<String> {
        println("lines!")
        return combine(v, v1, v2) { a, b, c ->
            println("combine! $a $b $c")
            a + b + c
        }//.onStart { println("start!"); emit(emptyList()) }
            .flatMapLatest { lines11(it) }
            .retry(3) { e ->
                val shallRetry = e is RuntimeException
                if (shallRetry) delay(100)
                println("retrying... $shallRetry\r")
                shallRetry
            }
            //.takeUntil(stopSubject)
            .onCompletion { cause -> if (cause == null) emit("INPUT HAS EXITED") else println("EXIT COMPLETE $cause\r") }
            //.flowOn(dispatcherIo)
            .onStart { println("start logcat lines\r") }
            .shareIn(
                scope,
                SharingStarted.Lazily,
                Config.LogLinesBufferCount,
            )
            .onSubscription { println("subscr to shared lines\r") }
            .onCompletion { println("shared compl!\r") }
    }

    override fun clear(): Boolean {
        println("clearing..")

        //use suspendCoroutine
        //For instance, you might use withContext to switch to a particular thread pool, or you might wrap a callback-based function using suspendCancellableCoroutine. Either way, calling those functions would force you to add the suspend modifier to your function.

        val childCode = Command("adb")
            .args("logcat", "-c")
            .spawn()
            .start()
        //.wait()

        println("exit code: ")

        return true
    }
}
