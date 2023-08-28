import LogcatState.WaitingInput
import app.cash.turbine.Turbine
import app.cash.turbine.test
import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class LogcatTest {

    //@Mock
    lateinit var dogcat: Logcat

    @BeforeTest fun beforeTest() {
        val ls = DummyLogSource() //LogcatSource()
        dogcat = Logcat(ls)
    }

    //use DI
    //inject test dispatchers
    @Test fun `start as waiting for log lines input`() = runTest {
        val state = dogcat.state.first()

        state shouldBe WaitingInput
    }

    @Test fun `begin capturing when input appears`() = runTest {
        dogcat.processCommand(StartupAs.All)

        //use test dispatcher instead
        delay(10)
        val state = dogcat.state.first()

        state.shouldBeInstanceOf<LogcatState.CapturingInput>()
    }

    @Test fun `log lines flow does not complete while input is active`() = runTest {
        dogcat.processCommand(StartupAs.All)

        //use test dispatcher instead
        delay(10)
        val state = dogcat.state.first() as LogcatState.CapturingInput
        val result = mutableListOf<LogLine>()

        //state.lines.toList(result)
        //result shouldHaveSize 10

        val j = launch {
            state.lines
                .onEach { println(it) }
                .collect()
        }

        println("some")
        delay(70000) // skipped

        println("delay")
        j.isActive shouldBe true
        j.cancelAndJoin()
        println("done")
    }

    @Test fun `stop input consumption upon unsubscribing`() = runTest {
        dogcat.processCommand(StartupAs.All)

        //use test dispatcher instead
        delay(10)
        val state = dogcat.state.drop(1).first() as LogcatState.CapturingInput
        val result = mutableListOf<LogLine>()

        val j = launch {
            state.lines
                .onEach { println(it) }
                .collect()
        }

        //dogcat.processCommand(StopEverything)

        delay(10)

        //j.isActive shouldBe false
        /*val job = launch {
            dogcat.sss
        }
        job.join()*/
    }

    @Test fun `get log lines if subscribed before launch`() = runTest {
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

    @Test fun `log lines are correctly parsed into segments`() = runTest {
        val job = launch {
            dogcat.sss
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
            dogcat.sss
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


    @Test fun `emit 'reset' state when input cleared`() = runTest {
        val job = launch {
            dogcat.state
                .take(2)
                .onEach {
                    println(it)
                    //it shouldBe LogcatState.InputCleared
                }
                .collect()
        }
        dogcat.processCommand(ClearLogs)

        job.join()
    }

    @Test fun `auto re-start log consumption after clearing log input`() = runTest {
        dogcat.processCommand(StartupAs.All)

        turbineScope {

            val t1 = dogcat.state.testIn(backgroundScope)

            t1.awaitItem().shouldBeInstanceOf<LogcatState.CapturingInput>()

            dogcat.processCommand(ClearLogs)

            t1.awaitItem() shouldBe LogcatState.InputCleared
            t1.awaitItem().shouldBeInstanceOf<LogcatState.CapturingInput>()
        }

        /*delay(100)

        dogcat.state.test {
            awaitItem().shouldBeInstanceOf<LogcatState.CapturingInput>()

            awaitItem() shouldBe LogcatState.InputCleared

            awaitItem().shouldBeInstanceOf<LogcatState.CapturingInput>()
        }*/


    }

    @Test fun `emit correct indices for warnings and errors`() = runTest {

        //maybe model as sequence -- on receiving side
    }

//double check correct parsing (leaking tags)

    //Tip: If your class creates coroutines that don't complete on their own and should be canceled at the end of the test, you can inject TestScope.backgroundScope instead of the TestScope itself.

    @Test fun `should return lines according to input filter`() = runTest {

    }

    @Test fun `should return all lines on empty input filter`() = runTest {

    }

    @Test fun `should keep all log levels upon startup`() = runTest {

    }

    @Test fun `should exclude log levels upon filtering`() =  runTest {
        dogcat.processCommand(StartupAs.All)
        dogcat.processCommand(Filter.ToggleLogLevel("D"))

        val job = launch {
            dogcat.sss
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

    //handle logcat restarts / emulator breaks
    @Test fun `reset to 'waiting input' if input source breaks and re-start logcat`() {

    }
}
