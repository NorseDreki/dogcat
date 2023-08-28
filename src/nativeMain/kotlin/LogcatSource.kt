import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

class LogcatSource : LogSource {
    val greenColor = "\u001b[31;1;4m"
    val reset = "\u001b[0m" // to reset color to the default
    val name = greenColor + "Alex" + reset // Add green only to Alex
//viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)

    override fun lines(): Flow<String> {
        println("11111 start LOGCAT")

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

        val childCode = Command("adb")
            .args("logcat", "-c")
            .spawn()
            .start()
        //.wait()

        println("exit code: ")

        return true
    }
}
