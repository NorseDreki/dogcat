package platform

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts

class DumpsysPackage {

    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO

    suspend fun parseUserIdFor(packageName: String) = withContext(dispatcherIo) {
        /*contract {
            require(packageName.isNotEmpty())
        }*/

        val UID_CONTEXT = """^Packages:\n[\s]+Package[\s]+\[$packageName\][\s]+\(.*\)\:\n[\s]+userId=([\d]*)${'$'}""".toRegex()

        val output = Command("adb")
            .args("shell", "dumpsys", "package")
            .arg(packageName)
            .stdout(Stdio.Pipe)
            .output()

        val userId = output?.let {
            val match = UID_CONTEXT.matchEntire(it)
            match?.let {
                val (userId) = it.destructured
                userId
            }
        }

        userId ?: throw RuntimeException("UserId not found!")
    }
}
