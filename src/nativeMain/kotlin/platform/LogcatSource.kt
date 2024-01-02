package platform

import Config
import Environment
import Logger
import dogcat.LogLinesSource
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import dogcat.InternalAppliedFiltersState
import dogcat.LogFilter.ByPackage
import dogcat.LogFilter.MinLogLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalStdlibApi::class)
class LogcatSource(
    private val state: InternalAppliedFiltersState,
    private val environment: Environment,
    private val dispatchersIO: CoroutineDispatcher = Dispatchers.IO,
) : LogLinesSource {

    override fun lines() : Flow<String> =
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

            val d = environment.devices()
            //println("1111 $d")


            val logcat = Command("adb")
                .args(
                    listOf("logcat", "-v", "brief", userId, minLogLevel)
                )
                .stdout(Stdio.Pipe)
                .spawn()


            val stdoutReader = logcat.bufferedStdout()!!// .getChildStdout()!!

            try {
                while (true) {
                    //println("1111111 line")
                    val line = stdoutReader.readLine() ?: break
                    emit(line)
                    //yield() //?
                }
            } catch (e: CancellationException) {
                Logger.d("!!!!!!!!!!! Cancellation! $e")
            } catch (e: RuntimeException) {
                Logger.d(
                    "!!!!!!!!!!! Runtime! ${e.message} [${(currentCoroutineContext()[CoroutineDispatcher])}]"
                )
            }

            Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] !!!!!!!!! Killing logcat ${currentCoroutineContext().isActive}")
            // tried timeout, need async IO so badly
            // command would be killed when next line appears.
            // also, no leftover adb upon app exit
            logcat.kill()
        }
}
