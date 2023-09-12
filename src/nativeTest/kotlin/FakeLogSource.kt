import dogcat.LogSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class FakeLogSource : LogSource {

    private val source = MutableSharedFlow<String>()
    private val source1 = MutableStateFlow("")

    suspend fun emitLine(line: String) {
        source.emit(line)
    }

    override fun lines() = flow {
        println("emitting lines")
        emit("1")
        println("em 1")
        delay(20)
        emit("2")
        println("em 2")
        delay(20)
        throw RuntimeException("Boom")
    }

    override fun clear(): Boolean {
        TODO("Not yet implemented")
    }
}