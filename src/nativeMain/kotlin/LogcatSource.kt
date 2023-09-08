import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

class LogcatSource : LogSource {

    override fun lines(): Flow<String> {
        return flow {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader: Reader? = child.getChildStdout()

            while (true) {
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
