package platform

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.*

class DumpsysPackage {

    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO

    suspend fun parseUserIdFor(packageName: String) = withContext(dispatcherIo) {
        //add timeout!

        /*contract {
            require(packageName.isNotEmpty())
        }*/

        val UID_CONTEXT = """Packages:\n\s+Package\s+\[$packageName]\s+\(.*\):\n\s+userId=(\d*)""".toRegex()

        println("waiting")
        //val output = try {

             val output = withTimeout(Config.AdbCommandTimeoutMillis) {
                Command("adb")
                    .args("shell", "dumpsys", "package")
                    .arg(packageName)
                    .stdout(Stdio.Pipe)
                    .output()
            }
        /*} catch (e: TimeoutCancellationException) {
            Logger.d("TImeou!")
        }*/

        Logger.d("got $output")

        val userId = output?.let {
            val match = UID_CONTEXT.find(it)
            match?.let {
                val (userId) = it.destructured
                userId
            }
        }

        userId ?: throw RuntimeException("UserId not found!")
    }
}
