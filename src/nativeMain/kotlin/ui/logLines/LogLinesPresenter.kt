package ui.logLines

import AppConfig.DEFAULT_TAG_WIDTH
import AppState
import com.norsedreki.dogcat.Dogcat
import com.norsedreki.dogcat.state.DogcatState.Active
import com.norsedreki.dogcat.state.DogcatState.Inactive
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ui.HasLifecycle
import userInput.AppArguments
import userInput.Input
import userInput.Keymap
import userInput.Keymap.Actions.*
import kotlin.coroutines.coroutineContext

class LogLinesPresenter(
    private val dogcat: Dogcat,
    private val appArguments: AppArguments,
    private val appState: AppState,
    private val input: Input,
) : HasLifecycle {

    //views can come and go, when input disappears
    private lateinit var view: LogLinesView

    override suspend fun start() {
        view = LogLinesView()
        //view.start()

        val scope = CoroutineScope(coroutineContext)

        scope.launch {
            collectLogLines()
        }
        scope.launch {
            collectKeypresses()
        }

        scope.launch {
            appState.state
                .map { it.autoscroll }
                .distinctUntilChanged()
                .collect {
                    if (it) {
                        view.end()
                    }
                }
        }

        scope.launch {
            appState.state
                .collect {
                    view.state = view.state.copy(
                        autoscroll = it.autoscroll,
                        showLineNumbers = appArguments.lineNumbers ?: false,
                        tagWidth = appArguments.tagWidth ?: DEFAULT_TAG_WIDTH,
                        isCursorHeld = it.isCursorHeld,
                        cursorReturnLocation = it.inputFilterLocation
                    )
                }
        }
    }

    override suspend fun stop() {
        view.stop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectLogLines() {
        dogcat
            .state
            .flatMapLatest {
                when (it) {
                    is Active -> {
                        Logger.d("${context()} Start capturing log lines")

                        it.lines
                    }

                    Inactive -> {
                        Logger.d("${context()} Stop capturing log lines")
                        view.clear()

                        emptyFlow()
                    }
                }
            }
            .buffer(0) //omg!
            .collect {
                view.processLogLine(it)
            }
    }

    private suspend fun collectKeypresses() {
        input
            .keypresses
            .collect {
                when (Keymap.bindings[it]) {
                    HOME -> {
                        appState.autoscroll(false)
                        view.home()
                    }

                    END -> {
                        appState.autoscroll(true)
                        view.end()
                    }

                    LINE_UP -> {
                        appState.autoscroll(false)
                        view.lineUp()
                    }

                    LINE_DOWN -> {
                        appState.autoscroll(false)
                        view.lineDown(1)
                    }

                    PAGE_DOWN -> {
                        appState.autoscroll(false)
                        view.pageDown()
                    }

                    PAGE_UP -> {
                        appState.autoscroll(false)
                        view.pageUp()
                    }

                    else -> {
                        // Other keys are handled elsewhere
                    }
                }
            }
    }
}
