import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest


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
        val j = launch {
            println("sssss111")
            dogcat.state
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
            dogcat.state
                .take(1)
                .onEach {
                    println("22222  $it")
                    assertTrue { true }
                }
                .launchIn(this)
        }
    }

    @Test fun `log lines are correctly parsed into segments`() = runTest {
        val job = launch {
            dogcat.state
                .take(DummyLogSource.lines.size)
                .withIndex()
                .onEach {
                    val s = DummyLogSource.lines[it.index]

                    val parsed = it.value
                    if (parsed is Parsed) {
                        s shouldContain parsed.message
                        s shouldContain parsed.level
                        s shouldContain parsed.owner
                        s shouldContain parsed.tag
                    }
                }
                .collect()
        }

        dogcat.processCommand(StartupAs.All)

        job.join()
    }

    @Test fun `return log line as is if parsing failed`() = runTest {
        val job = launch {
            dogcat.state
                .take(DummyLogSource.lines.size)
                .withIndex()
                .onEach {
                    val s = DummyLogSource.lines[it.index]

                    val parsed = it.value
                    if (parsed is Original) {
                        s shouldBe parsed.line
                    }
                }
                .collect()
        }

        dogcat.processCommand(StartupAs.All)

        job.join()
    }

    @Test fun `should start capturing when input appears`() = runTest {

    }

    @Test fun `emit 'reset' state when input cleared`() = runTest {

    }

    @Test fun `emit correct indices for warnings and errors`() = runTest {

    }



    @Test fun `should return lines according to input filter`() = runTest {

    }

    @Test fun `should return all lines on empty input filter`() = runTest {

    }

    @Test fun `should keep all log levels upon startup`() = runTest {

    }

    @Test fun `should exclude log levels upon filtering`() =  runTest {
        dogcat.processCommand(StartupAs.All)
        dogcat.processCommand(Filter.ByLogLevel("D"))

        val job = launch {
            dogcat.state
                .take(3)
                .withIndex()
                .onEach {
                    val parsed = it.value
                    if (parsed is Parsed) {
                        println(parsed.level)
                        parsed.level shouldNotBe "D"
                    }
                }
                .collect()
        }

        job.join()
    }

    @Test fun `stop input consumption upon unsubscribing`() = runTest {

    }
}