package platform

import Config
import Logger
import dogcat.LogLinesSource
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import dogcat.InternalAppliedFiltersState
import dogcat.LogFilter.ByPackage
import dogcat.LogFilter.MinLogLevel
import io.ktor.utils.io.core.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalStdlibApi::class)
class LogcatSource(
    private val state: InternalAppliedFiltersState,
    private val dispatchersIO: CoroutineDispatcher = Dispatchers.IO,
) : LogLinesSource {

    override fun lines() =
        flow {
            val af = state.applied.value

            val minLogLevel =
                af[MinLogLevel::class]?.let { "*:${(it as MinLogLevel).logLevel}" } ?: ""
            val pkgE = true
            val userId = if (pkgE) {
                af[ByPackage::class]?.let { "--uid=${(it as ByPackage).resolvedUserId}" }
                    ?: ""
            } else {
                ""
            }
            Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Starting adb logcat")

            val logcat = Command("adb")
                .args("logcat", "-v", "brief")
                .args(userId, minLogLevel)
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader = logcat.getChildStdout()!!

            try {
                while (true) {
                    val line = stdoutReader.readLine() ?: break
                    emit(line)
                    //yield() //?
                }
            } catch (e: MalformedUTF8InputException) {
                Logger.d("!!!!!!!!!!! MalformedUTF8InputException! ${e.message} [${(currentCoroutineContext()[CoroutineDispatcher])}]")
            } catch (e: CancellationException) {
                Logger.d("!!!!!!!!!!! Cancellation! $e")
            }

            Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] !!!!!!!!! Killing logcat ${currentCoroutineContext().isActive}")
            // tried timeout, need async IO so badly
            // command would be killed when next line appears.
            // also, no leftover adb upon app exit
            logcat.kill()
        }
}
