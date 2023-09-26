package platform

import dogcat.LogLinesSource
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import dogcat.InternalAppliedFiltersState
import dogcat.LogFilter
import io.ktor.utils.io.core.*
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
                af[LogFilter.MinLogLevel::class]?.first?.let { "*:${(it as LogFilter.MinLogLevel).logLevel}" } ?: ""
            val pkgE = af[LogFilter.ByPackage::class]?.second ?: false
            val userId = if (pkgE) {
                af[LogFilter.ByPackage::class]?.first?.let { "--uid=${(it as LogFilter.ByPackage).resolvedUserId}" }
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
                    //stdoutReader.read
                    val line = stdoutReader.readUTF8Line(200) ?: break
                    //val line = stdoutReader.readLine() ?: break
                    emit(line)
                    //yield() //?
                }
            } catch (e: MalformedUTF8InputException) {
                //e.printStackTrace()
                Logger.d("!!!!!!!!!!! Malformed UTF8! ${e.message}")
                //cancel()

            } catch (e: CancellationException) {
                Logger.d("!!!!!!!!!!! Cancellation! $e")
            }

            Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] !!!!!!!!! Killing logcat ${currentCoroutineContext().isActive}")
            // tried timeout, need async IO so badly
            // command would be killed when next line appears.
            // also, no leftover adb upon app exit
            logcat.kill()
        }
        .flowOn(dispatchersIO)


    override suspend fun clear(): Boolean {
        val childStatus = withContext(dispatchersIO) {
            withTimeout(Config.AdbCommandTimeoutMillis) {
                Command("adb")
                    .args("logcat", "-c")
                    .status()
            }
        }

        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Exit code for 'adb logcat -c': ${childStatus.code}")

        return childStatus.code == 0
    }
}
