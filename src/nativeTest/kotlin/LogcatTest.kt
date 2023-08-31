import LogcatState.CapturingInput
import LogcatState.WaitingInput
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class LogcatTest {
/*
    val dogcatModule = DI.Module("dogcat") {
        bindSingleton<LogSource> { DummyLogSource() }
        bind<Logcat> { Logcat(instance()) }
    }

    val di = DI {
        import(dogcatModule)
    }
*/

    //@Mock
    private lateinit var dogcat: Logcat
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher : CoroutineDispatcher = StandardTestDispatcher(scheduler)
    val handler = CoroutineExceptionHandler { _, t -> println("999999 ${t.message}") }
    var thr: Throwable? = null

    @BeforeTest fun beforeTest() {
        val ls = DummyLogSource()
        dogcat = Logcat(ls, dispatcher, dispatcher)
    }

    @Test fun `start as waiting for log lines input`() = runTest(dispatcher) {
        dogcat.state.test {
            awaitItem() shouldBe WaitingInput
        }
    }

    @Test fun `begin capturing when input appears`() = runTest(dispatcher) {
        dogcat(StartupAs.All)
        advanceUntilIdle()

        dogcat.state.test {
            //assert a call to lines() has occured?
            awaitItem().shouldBeInstanceOf<CapturingInput>()
        }
    }

    @Test fun `get log lines if subscribed before launch`() = runTest(dispatcher) {
        launch {
            val expectCapturingInput = dogcat.state.drop(1).first()

            expectCapturingInput.shouldBeInstanceOf<CapturingInput>()
        }
        advanceUntilIdle()

        dogcat(StartupAs.All)
    }

    @Test fun `capture all input lines without loss`() = runTest(dispatcher) {
        dogcat(StartupAs.All)
        advanceUntilIdle()

        dogcat.state.test {
            val input = awaitItem() as CapturingInput

            input.lines.test {
                DummyLogSource.lines.forEach {
                    val logLine = awaitItem()

                    when (logLine) {
                        is Parsed -> {
                            it shouldContain logLine.message
                            it shouldContain logLine.level
                            it shouldContain logLine.owner
                            it shouldContain logLine.tag
                        }
                        is Original -> {
                            it shouldBe logLine.line
                        }
                    }
                }
                awaitItem()
                //awaitComplete()
                expectNoEvents()
                this.ensureAllEventsConsumed()
            }
        }
    }

    @Test fun `collectors should receive respective events`() = runTest(dispatcher) {
        launch {
            dogcat.state.test {
                awaitItem() shouldBe WaitingInput
                awaitItem().shouldBeInstanceOf<CapturingInput>()
                ensureAllEventsConsumed()
            }
        }
        advanceUntilIdle() //maybe not needed

        dogcat(StartupAs.All)
        advanceUntilIdle()

        dogcat.state.test {
            awaitItem().shouldBeInstanceOf<CapturingInput>()
            ensureAllEventsConsumed()
        }
    }

    @Test fun `return log line as is if parsing failed`() = runTest {

    }

    @Test fun `log lines are correctly parsed into segments`() = runTest {
    }


    @Test fun `log lines flow does not complete while input is active`() = runTest {
        dogcat(StartupAs.All)

        //use test dispatcher instead
        delay(10)
        val state = dogcat.state.first() as CapturingInput
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
        dogcat(StartupAs.All)

        //use test dispatcher instead
        delay(10)
        val state = dogcat.state.drop(1).first() as CapturingInput
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
        dogcat(ClearLogs)

        job.join()
    }

    @Test fun `auto re-start log consumption after clearing log input`() = runTest(dispatcher) {
        dogcat(StartupAs.All)
        advanceUntilIdle()

        dogcat.state.test {
            awaitItem().shouldBeInstanceOf<CapturingInput>()

            dogcat(ClearLogs)

            awaitItem() shouldBe LogcatState.InputCleared
            awaitItem().shouldBeInstanceOf<CapturingInput>()
        }
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

    @Test fun `should exclude log levels upon filtering`() =  runTest(dispatcher) {
        dogcat(StartupAs.All)
        dogcat(Filter.ToggleLogLevel("D"))
        advanceUntilIdle()

        dogcat.state.test {
            //skipItems(1)

            val lines = (awaitItem() as CapturingInput).lines

            lines
                .take(3)
                .onEach {
                    val parsed = it
                    if (parsed is Parsed) {
                        println(parsed.level)
                        parsed.level shouldNotBe "D"
                    }
                }
        }
    }

    //handle logcat restarts / emulator breaks
    @Test fun `reset to 'waiting input' if input source breaks and re-start logcat`() = runTest(dispatcher) {
        val ls = FakeLogSource()
        val dogcat = Logcat(ls, dispatcher, dispatcher)
        println("zzzzzzz ${this.coroutineContext[CoroutineExceptionHandler]}")

        launch(handler) {
            turbineScope {
                val t = dogcat.state.testIn(backgroundScope)
                t.awaitItem() shouldBe WaitingInput

                dogcat(StartupAs.All)
                val c = t.awaitItem() as CapturingInput

                c.lines.test {
                    awaitItem() shouldBe Original("1")
                    awaitItem() shouldBe Original("2")
                    awaitItem() shouldBe Original("1")
                    awaitItem() shouldBe Original("2")
                    awaitItem() shouldBe Original("1")
                    awaitItem() shouldBe Original("2")
                    awaitItem() shouldBe Original("1")
                    awaitItem() shouldBe Original("2")
                    //awaitError()
                }
            }
        }
    }
}
