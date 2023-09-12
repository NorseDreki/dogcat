import dogcat.LogSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class Fake2LogSource : LogSource {

    private val source = MutableSharedFlow<String>()
    private val source1 = MutableStateFlow("")

    suspend fun emitLine(line: String) {
        println("emitting line...")
        source.emit(line)

        println("emitted $line")
        delay(10)
    }

    override fun lines() = source.asSharedFlow()

    override fun clear(): Boolean {
        TODO("Not yet implemented")
    }
}