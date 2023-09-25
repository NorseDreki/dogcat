import dogcat.PublicState.CapturingInput
import dogcat.PublicState.WaitingInput
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import dogcat.*
import dogcat.Command.Start
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogcatTest {
/*
    val dogcatModule = DI.Module("dogcat") {
        bindSingleton<dogcat.LogSource> { DummyLogSource() }
        bind<Logcat> { Logcat(instance()) }
    }

    val di = DI {
        import(dogcatModule)
    }
*/

    private lateinit var dogcat: Dogcat
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher : CoroutineDispatcher = StandardTestDispatcher(scheduler)

    @BeforeTest fun beforeTest() {
        val ls = DummyLogSource()
        dogcat = Dogcat(ls, InternalAppliedFiltersState(), dispatcher, dispatcher)
    }

    @Test fun `start as waiting for log lines input`() = runTest(dispatcher) {
        dogcat.state.test {
            awaitItem() shouldBe WaitingInput
            expectNoEvents()
        }
    }

    @Test fun `begin capturing when input appears`() = runTest(dispatcher) {
        dogcat(Start.All)
        advanceUntilIdle()

        dogcat.state.test {
            // if input fails upon 3 attempts, re-throw that exception into shared flow
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

        dogcat(Start.All)
    }

    @Test fun `capture all input lines without loss`() = runTest(dispatcher) {
        dogcat(Start.All)
        advanceUntilIdle()

        dogcat.state.test {
            val input = awaitItem() as CapturingInput

            input.lines.test {
                DummyLogSource.lines.forEach {
                    /*when (val logLine = awaitItem()) {
                        is LogLine -> {
                            it shouldContain logLine.message
                            it shouldContain logLine.level
                            it shouldContain logLine.owner
                            it shouldContain logLine.tag
                        }
                        is Original -> {
                            it shouldBe logLine.line
                        }
                    }*/
                }
                awaitItem()
                expectNoEvents()
            }
        }
    }

    @Test fun `complete previous 'lines' upon emission of new ones`() = runTest(dispatcher) {
        dogcat(Start.All)
        advanceUntilIdle()

        dogcat.state.test {
            val input = awaitItem() as CapturingInput

            //launch {
                input.lines.test {
                    DummyLogSource.lines.forEach {
                        awaitItem()
                    }
                    awaitItem()

                    dogcat(Command.ClearLogSource)
                    //awaitItem()
                    awaitComplete()
                    //expectNoEvents()
                }
            //}
            awaitItem()
            val input1 = awaitItem() as CapturingInput

            input1.lines.test {
                DummyLogSource.lines.forEach {
                    awaitItem()
                }
                awaitItem()
                expectNoEvents()
                //awaitComplete()
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

        dogcat(Start.All)
        advanceUntilIdle()

        dogcat.state.test {
            awaitItem().shouldBeInstanceOf<CapturingInput>()
            ensureAllEventsConsumed()
        }
    }
/*
    @Test fun `return log line as is if parsing failed`() = runTest {

    }

    @Test fun `log lines are correctly parsed into segments`() = runTest {
    }
*/

    @Test fun `log lines flow does not complete while input is active`() = runTest(dispatcher) {
        val ls = Fake2LogSource()
        val dogcat1 = Dogcat(ls, InternalAppliedFiltersState(), dispatcher, dispatcher)

        dogcat1(Start.All)
        advanceUntilIdle()

        dogcat1.state.test {
            val items = awaitItem() as CapturingInput

            val j = launch {
                items.lines
                    .collect {
                        println("collected")
                    }
            }
            advanceUntilIdle()
            ls.emitLine("1")

            delay(5000)
            j.isActive shouldBe true
            println("active")
            j.cancelAndJoin()

            /*items.lines.test {
                advanceUntilIdle()

                ls.emitLine("1")
                awaitItem()
            }*/
        }
    }

    @Test fun `stop input consumption upon unsubscribing`() = runTest(dispatcher) {
        dogcat(Start.All)
        advanceUntilIdle()

        dogcat.state.test {
            val items = awaitItem() as CapturingInput

            launch(UnconfinedTestDispatcher(testScheduler)) {
                items.lines.collect {
                    println("coll $it")
                }
                println("end")
            }

            dogcat(Command.Stop)
            advanceUntilIdle()

            awaitItem() shouldBe PublicState.Stopped
        }
    }

    @Test fun `auto re-start log consumption after clearing log input`() = runTest(dispatcher) {
        dogcat(Start.All)
        advanceUntilIdle()

        dogcat.state.test {
            awaitItem().shouldBeInstanceOf<CapturingInput>()

            dogcat(Command.ClearLogSource)

            awaitItem() shouldBe PublicState.InputCleared
            val input = awaitItem() as CapturingInput

            input.lines.test {
                DummyLogSource.lines.forEach {
                    /*when (val logLine = awaitItem()) {
                        is LogLine -> {
                            it shouldContain logLine.message
                            it shouldContain logLine.level
                            it shouldContain logLine.owner
                            it shouldContain logLine.tag
                        }

                        is Original -> {
                            it shouldBe logLine.line
                        }
                    }*/
                }
                awaitItem()
            }
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

    @Test fun `exclude log levels upon filtering`() =  runTest(dispatcher) {
        dogcat(Start.All)
        //dogcat(Filter.ToggleLogLevel("D"))
        advanceUntilIdle()

        dogcat.state.test {
            val lines = (awaitItem() as CapturingInput).lines

            lines
                .take(3)
                .onEach {
                    /*val parsed = it
                    if (parsed is LogLine) {
                        println(parsed.level)
                        parsed.level shouldNotBe "D"
                    }*/
                }
        }
    }

    @Test fun `reset to 'waiting input' if emulator breaks and re-start logcat`() = runTest(dispatcher) {
        val ls = FakeLogSource()
        val dogcat = Dogcat(ls, InternalAppliedFiltersState(), dispatcher, dispatcher)

        turbineScope {
            val t = dogcat.state.testIn(backgroundScope)
            t.awaitItem() shouldBe WaitingInput

            dogcat(Start.All)
            val c = t.awaitItem() as CapturingInput

            c.lines.test {
                /*awaitItem() shouldBe Original("1")
                awaitItem() shouldBe Original("2")
                awaitItem() shouldBe Original("1")
                awaitItem() shouldBe Original("2")
                awaitItem() shouldBe Original("1")
                awaitItem() shouldBe Original("2")
                awaitItem() shouldBe Original("1")
                awaitItem() shouldBe Original("2")*/
            }
        }
    }
}
