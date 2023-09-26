package platform

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object EmulatorName {

    suspend fun currentEmulatorName() = withContext(Dispatchers.IO) {
        val output = withTimeout(Config.AdbCommandTimeoutMillis) {
            Command("adb")
                .args("emu", "avd", "name")
                .stdout(Stdio.Pipe)
                .output()
                ?.lines()
                ?.first()
        }

        return@withContext output
    }
}
