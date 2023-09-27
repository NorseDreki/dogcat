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
import ncurses.napms
import kotlin.text.CharCategory.LINE_SEPARATOR

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
                .args("logcat", "-v", "brief", "-d")
                .args(userId, minLogLevel)
                .stdout(Stdio.Pipe)
                .spawn()

            /*val logcat = Command("cat")
                .args("unicode_bug.txt")
                .stdout(Stdio.Pipe)
                .spawn()*/

            val stdoutReader = logcat.getChildStdout()!!

            try {
                while (true) {
/*                    LINE_SEPARATOR
                    val dst = ByteArray(1000) //{ 'z'.code.toByte() }
                    val num1 = stdoutReader.readAvailable(dst)
                    val dst1 = stdoutReader.readText()
                    val line = dst1*/
                    /*val num1 = stdoutReader.readUntilDelimiter(0x0A, dst)
                    val line = dst.decodeToString()*/
                    val line = stdoutReader.readUTF8Line() ?: break
                    //Logger.d("------> [${(currentCoroutineContext()[CoroutineDispatcher])}] $line")

                    emit(line)
                   // delay(5)
                    //yield() //?

                    //napms(15)
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

    override suspend fun clear(): Boolean {
        val childStatus = withContext(dispatchersIO) {
            withTimeout(Config.AdbCommandTimeoutMillis) { //won't work
                Command("adb")
                    .args("logcat", "-c")
                    .status()
            }
        }

        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Exit code for 'adb logcat -c': ${childStatus.code}")

        return childStatus.code == 0
    }
}
