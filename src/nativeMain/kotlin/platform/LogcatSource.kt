package platform

import dogcat.LogLinesSource
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import dogcat.InternalQuery
import dogcat.LogFilter
import io.ktor.utils.io.core.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LogcatSource(
    private val state: InternalQuery,
    private val dispatchersIO: CoroutineDispatcher = Dispatchers.IO,
) : LogLinesSource {

    override fun lines() =
        flow {
            val af = state.appliedFilters.value

            Logger.d("99999 $af")
            val minLogLevel =
                af[LogFilter.MinLogLevel::class]?.first?.let { "*:${(it as LogFilter.MinLogLevel).logLevel}" } ?: ""
            val pkgE = af[LogFilter.ByPackage::class]?.second ?: false
            val userId = if (pkgE) {
                af[LogFilter.ByPackage::class]?.first?.let { "--uid=${(it as LogFilter.ByPackage).resolvedUserId}" }
                    ?: ""
            } else {
                ""
            }
            Logger.d("=========")

            val logcat = Command("adb")
                .args("logcat", "-v", "brief")
                .args(userId, minLogLevel)
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader = logcat.getChildStdout()!!

            try {
                while (true) {
                    //UTF-8 only
                    val line = stdoutReader.readLine() ?: break
                    emit(line)
                    yield() //?
                }
            } catch (e: MalformedUTF8InputException) {
                //e.printStackTrace()
                Logger.d("!!!!!!!!!!! Malformed UTF8! ${e.message}")
                //cancel()

            } catch (e: CancellationException) {
                Logger.d("!!!!!!!!!!! Cancellation! $e")
            } finally {

            }

            Logger.d("!!!!!!!!! Killing logcat ${currentCoroutineContext().isActive}")
            // withTimeout()?
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

        Logger.d("Exit code for 'adb logcat -c': ${childStatus.code}")

        return childStatus.code == 0
    }
}
