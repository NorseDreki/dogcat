import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield


class LogcatTest {

    //@Mock
    lateinit var dogcat: Logcat

    @BeforeTest
    fun beforeTest() {
        dogcat = Logcat()
    }

    //use DI
    //inject test dispatchers
    @Test
    fun `should start as waiting for input`() = runTest {
        launch(Dispatchers.Default) {
            dogcat.sss
                .onEach {
                    println("$it")
                    yield()
                    //assertTrue { true }
                }
                .launchIn(this)
        }

        launch(Dispatchers.Default) {
            dogcat.processCommand(StartupAs.All)
        }
    }

    @Test fun `should get items if subscribed before start`() = runTest {

    }

    @Test fun `should get replayed items if subscribed after start`() = runTest {

    }

    @Test fun `should start capturing when input appears`() = runTest {

    }

    @Test fun `emit 'reset' state when input cleared`() = runTest {

    }

    @Test fun `emit correct indices for warnings and errors`() = runTest {

    }

    @Test fun `parse log line correctly`() = runTest {

    }

    @Test fun `return log line as is if parsing failed`() = runTest {

    }

    @Test fun `should return lines according to input filter`() = runTest {

    }

    @Test fun `should return all lines on empty input filter`() = runTest {

    }

    @Test fun `should keep all log levels upon startup`() = runTest {

    }

    @Test fun `should exclude log levels upon filtering`() =  runTest {

    }

    @Test fun `stop input consumption upon unsubscribing`() = runTest {

    }
}