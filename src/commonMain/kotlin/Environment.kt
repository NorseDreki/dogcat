import kotlinx.coroutines.flow.Flow

interface Environment {

    fun lines(minLogLevel: String, userId: String = "") : Flow<String>

    suspend fun userIdFor(packageName: String): String

    suspend fun currentEmulatorName(): String?

    suspend fun foregroundPackageName(): String

    suspend fun clearSource(): Boolean

    suspend fun devices(): List<String>

    fun heartbeat(): Flow<Boolean>
}
