package dogcat

import kotlinx.coroutines.flow.Flow

interface Shell {

    fun lines(minLogLevel: String, userId: String) : Flow<String>

    suspend fun appIdFor(packageName: String): String

    suspend fun currentEmulatorName(): String?

    suspend fun foregroundPackageName(): String

    suspend fun clearSource(): Boolean

    suspend fun devices(): List<String>

    fun heartbeat(): Flow<Boolean>

    suspend fun isShellAvailable(): Boolean
}

