package platform

import dogcat.LogSource
import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import dogcat.LogFilter
import dogcat.LogFilter.ByExcludedTags
import dogcat.State
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.yield

class LogcatSource(
    val state: State
) : LogSource {

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
        .distinctUntilChanged()

    val v1 = state.appliedFilters
        .filter { it.containsKey(LogFilter.MinLogLevel::class) }
        .distinctUntilChanged()

    val v2 = state.appliedFilters
        .filter { it.containsKey(LogFilter.ByTime::class) }
        .distinctUntilChanged()

    override fun lines(): Flow<String> {
        return flow {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
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
