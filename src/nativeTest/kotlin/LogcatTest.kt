import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield


class LogcatTest {

    //@Mock
    lateinit var dogcat: Logcat

    @BeforeTest
    fun beforeTest() {
        val ls = DummyLogSource() //LogcatSource()
        dogcat = Logcat(ls)
    }

    //use DI
    //inject test dispatchers
    @Test
    fun `should start as waiting for input`() = runTest {
        /*launch(Dispatchers.Default) {
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
        }*/
    }

    @Test fun `log lines flow does not complete`() = runTest {

    }

    @Test fun `get log lines if subscribed before launch`() = runTest {

        println("sssss")


        val j = launch {
            println("sssss111")
            dogcat.sss
                .take(8)
                .onEach {
                    println("22222  $it")
                    //assertTrue { true }
                }
                .onCompletion { println("completed") }
                .catch {  }
                .collect()
        }

        println("pr comm")
        //launch {
        dogcat.processCommand(StartupAs.All)
        //}

        println("after pr comm")

        //yield()
        j.join()
    }

    @Test fun `get replayed items if subscribed after start`() = runTest {
        launch(Dispatchers.Default) {
            dogcat.processCommand(StartupAs.All)
        }

        launch(Dispatchers.Default) {
            dogcat.sss
                .take(1)
                .onEach {
                    println("22222  $it")
                    assertTrue { true }
                }
                .launchIn(this)
        }
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