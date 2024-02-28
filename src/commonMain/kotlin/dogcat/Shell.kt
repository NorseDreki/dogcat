package dogcat

import kotlinx.coroutines.flow.Flow

interface Shell {

    fun logLines(minLogLevel: String, appId: String) : Flow<String>

    fun deviceRunning(): Flow<Boolean>

    suspend fun appIdFor(packageName: String): String

    suspend fun deviceName(): String

    suspend fun foregroundPackageName(): String

    suspend fun clearLogLines()

    suspend fun firstRunningDevice(): String

    suspend fun validateShellOrThrow()
}
