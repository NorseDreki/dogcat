import com.norsedreki.dogcat.state.PublicState.Active
import com.norsedreki.dogcat.state.PublicState.WaitingInput
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.norsedreki.dogcat.Command
import dogcat.*
import com.norsedreki.dogcat.Command.Start
import com.norsedreki.dogcat.Dogcat
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import com.norsedreki.dogcat.LogLineBriefParser
import com.norsedreki.dogcat.LogLines
import com.norsedreki.dogcat.state.DefaultAppliedFiltersState
import com.norsedreki.dogcat.state.PublicState
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
        val s = DefaultAppliedFiltersState()
        val lp = LogLineBriefParser()

        dogcat = Dogcat(s, LogLines(ls, lp, s, dispatcher, dispatcher))
    }

    @Test fun `start as waiting for log lines input`() = runTest(dispatcher) {
        dogcat.state.test {
            awaitItem() shouldBe WaitingInput
            expectNoEvents()
        }
    }

    @Test fun `begin capturing when input appears`() = runTest(dispatcher) {
        dogcat(Start.PickAllApps)
        advanceUntilIdle()

        dogcat.state.test {
            // if input fails upon 3 attempts, re-throw that exception into shared flow
            //assert a call to lines() has occured?
            awaitItem().shouldBeInstanceOf<Active>()
        }
    }

    @Test fun `get log lines if subscribed before launch`() = runTest(dispatcher) {
        launch {
            val expectCapturingInput = dogcat.state.drop(1).first()

            expectCapturingInput.shouldBeInstanceOf<Active>()
        }
        advanceUntilIdle()

        dogcat(Start.PickAllApps)
    }

    @Test fun `capture all input lines without loss`() = runTest(dispatcher) {
        dogcat(Start.PickAllApps)
        advanceUntilIdle()

        dogcat.state.test {
            val input = awaitItem() as Active

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
        dogcat(Start.PickAllApps)
        advanceUntilIdle()

        dogcat.state.test {
            val input = awaitItem() as Active

            //launch {
                input.lines.test {
                    DummyLogSource.lines.forEach {
                        awaitItem()
                    }
                    awaitItem()

                    dogcat(Command.ClearLogs)
                    //awaitItem()
                    awaitComplete()
                    //expectNoEvents()
                }
            //}
            awaitItem()
            val input1 = awaitItem() as Active

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
                awaitItem().shouldBeInstanceOf<Active>()
                ensureAllEventsConsumed()
            }
        }
        advanceUntilIdle() //maybe not needed

        dogcat(Start.PickAllApps)
        advanceUntilIdle()

        dogcat.state.test {
            awaitItem().shouldBeInstanceOf<Active>()
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
        //val dogcat1 = Dogcat(ls, InternalAppliedFiltersState(), dispatcher, dispatcher)

        dogcat(Start.PickAllApps)
        advanceUntilIdle()

        dogcat.state.test {
            val items = awaitItem() as Active

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
        dogcat(Start.PickAllApps)
        advanceUntilIdle()

        dogcat.state.test {
            val items = awaitItem() as Active

            launch(UnconfinedTestDispatcher(testScheduler)) {
                items.lines.collect {
                    println("coll $it")
                }
                println("end")
            }

            dogcat(Command.Stop)
            advanceUntilIdle()

            awaitItem() shouldBe PublicState.Terminated
        }
    }

    @Test fun `auto re-start log consumption after clearing log input`() = runTest(dispatcher) {
        dogcat(Start.PickAllApps)
        advanceUntilIdle()

        dogcat.state.test {
            awaitItem().shouldBeInstanceOf<Active>()

            dogcat(Command.ClearLogs)

            awaitItem() shouldBe PublicState.Inactive
            val input = awaitItem() as Active

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
        dogcat(Start.PickAllApps)
        //dogcat(Filter.ToggleLogLevel("D"))
        advanceUntilIdle()

        dogcat.state.test {
            val lines = (awaitItem() as Active).lines

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
        //val dogcat = Dogcat(ls, InternalAppliedFiltersState(), dispatcher, dispatcher)

        turbineScope {
            val t = dogcat.state.testIn(backgroundScope)
            t.awaitItem() shouldBe WaitingInput

            dogcat(Start.PickAllApps)
            val c = t.awaitItem() as Active

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
