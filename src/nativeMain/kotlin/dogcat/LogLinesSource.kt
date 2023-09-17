package dogcat

import kotlinx.coroutines.flow.Flow

interface LogLinesSource {

    fun lines(): Flow<String>

    suspend fun clear(): Boolean
}
