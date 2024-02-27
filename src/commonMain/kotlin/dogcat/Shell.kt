package dogcat

import kotlinx.coroutines.flow.Flow

interface Shell {

    fun lines(minLogLevel: String, userId: String) : Flow<String>

    fun deviceRunning(): Flow<Boolean>

    suspend fun appIdFor(packageName: String): String

    suspend fun deviceLabel(): String

    suspend fun foregroundPackageName(): String

    suspend fun clearSource()

    suspend fun devices(): String

    suspend fun validateShellOrThrow()
}
