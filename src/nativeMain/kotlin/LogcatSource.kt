import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

class LogcatSource : LogSource {

    override fun lines(): Flow<String> {
        println("11111 start LOGCAT")

        return flow {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader: Reader? = child.getChildStdout()

            while (true) {
                val line2 = stdoutReader!!.readLine() ?: break
                emit(line2)
                //if (isActive)
                yield()
            }
        }
    }
}
