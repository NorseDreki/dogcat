import kotlinx.coroutines.flow.Flow

interface LogSource {

    //UTF-8 only
    fun lines(): Flow<String>

    // or to be suspendable?
    fun clear(): Boolean
}
