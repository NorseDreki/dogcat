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
}