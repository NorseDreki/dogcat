package platform

import dogcat.LogSource
import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import dogcat.InternalState
import dogcat.LogFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LogcatSource(
    val state: InternalState
) : LogSource {

    override fun lines(): Flow<String> {
        return flow {

            val af = state.appliedFilters.value

            println("99999 $af")
            val mll = af[LogFilter.MinLogLevel::class]?.first?.let { "*:${(it as LogFilter.MinLogLevel).logLevel}" } ?: ""
            val pkgE = af[LogFilter.ByPackage::class]?.second ?: false
            val pkg = if (pkgE) {
                af[LogFilter.ByPackage::class]?.first?.let { "--uid=${(it as LogFilter.ByPackage).resolvedUserId}" } ?: ""
            } else {
                ""
            }
            println("=========")

            //println("99999 $af, $mll, $pkg")

            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .args(pkg, mll)
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
