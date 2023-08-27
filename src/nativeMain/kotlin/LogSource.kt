import kotlinx.coroutines.flow.Flow

interface LogSource {

    //UTF-8 only
    fun lines(): Flow<String>

    fun clear(): Boolean
}
