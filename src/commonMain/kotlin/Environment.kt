interface Environment {

    suspend fun userIdFor(packageName: String): String

    suspend fun currentEmulatorName(): String?

    suspend fun foregroundPackageName(): String

    suspend fun clearSource(): Boolean

    suspend fun devices(): List<String>
}
