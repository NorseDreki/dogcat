@file:OptIn(ExperimentalForeignApi::class)

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*
import ncurses.*
import okio.*
import platform.posix.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

val r2 = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

val prefix = "\\033[31;1;4m"
val postfix = "\\033[0m"


val sqWidth = 10
val sqHeight = 10

// HomeViewModel.kt

/*
private val currentSearchQuery = MutableStateFlow("")
private val isLoadingAutofillSuggestions = MutableStateFlow(false)
private val isLoadingSavedLocations = MutableStateFlow(false)
// saved locations are fetched from the local database
private val weatherDetailsOfSavedLocations = weatherRepository.getSavedLocationsListStream()
// whenever the current search query changes, this flow will fetch the suggested places
// for that query.
private val autofillSuggestions = currentSearchQuery.debounce(250)
    .distinctUntilChanged()
    .filter { it.isNotBlank() }
    .mapLatest { query ->
        isLoadingAutofillSuggestions.value = true
        locationServicesRepository.fetchSuggestedPlacesForQuery(query)
            .also { isLoadingAutofillSuggestions.value = false }
    }

val uiState = combine(
    isLoadingSavedLocations, // state flow
    isLoadingAutofillSuggestions, // state flow
    weatherDetailsOfSavedLocations, // flow
    autofillSuggestions // flow
) { isLoadingSavedLocations, isLoadingAutofillSuggestions, weatherDetailsOfSavedLocations, autofillSuggestions ->
    HomeScreenUiState(.....)
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(300),
    initialValue = HomeScreenUiState(isLoadingSavedLocations = true)
)
*/

val board = mutableListOf<CPointer<WINDOW>?>()

fun show() {
    /*initscr()
    defer { endwin() }
    noecho()
    curs_set(0)
    halfdelay(2)
*/
    initscr()
    noecho()
    cbreak()
    refresh()

    var starty = 0
    for (i in 0..10) {
        board.add(newwin(sqHeight, sqWidth, starty, i * sqWidth))
    }
    starty = sqHeight
    for (i in 0..10) {
        board.add(newwin(sqHeight, sqWidth, starty, i * sqWidth))
    }
    starty = sqHeight * 2
    for (i in 0..10) {
        board.add(newwin(sqHeight, sqWidth, starty, i * sqWidth))
    }

    for (window in board) {
        if (window == null) {
            println("Window was null!!")
        } else {
            box(window, 0U, 0U);
            wrefresh(window);
        }
    }

    getch()
}

@OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
fun main(): Unit = memScoped {
    setlocale(LC_CTYPE, "")
    //setlocale(LC_ALL, "en_US.UTF-8");
    initscr();
    savetty();
    noecho();//disable auto-echoing
    cbreak();//making getch() work without a buffer I.E. raw characters
    keypad(stdscr, true);//allows use of special keys, namely the arrow keys
    clear();    // empty the screen
    timeout(0); // reads do not block

    //nonl() as Unit /* tell curses not to do NL->CR/NL on output */
    //cbreak() as Unit /* take input chars one at a time, no wait for \n */
    //noecho() as Unit /* don't echo input */
    //if (!single_step) nodelay(stdscr, TRUE)
    //idlok(stdscr, TRUE) /* allow use of insert/delete line */

    val fp = newpad(32767, 120)
    scrollok(fp, true)
    //keypad(fp, true)

    /*(1..100).forEach {
        //wprintw(fp, "%d - line ------------------------------------------------------------------------  \n", it);
        waddstr(fp,"*** PROCESS $it *** \n")
    }

    prefresh(fp,25, 0, 10,0, 40,120);*/

    runBlocking {

        val lg = flow {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader: com.kgit2.io.Reader? = child.getChildStdout()

            while (true) {
                val line2 = stdoutReader!!.readLine() ?: break
                //delay(1.microseconds)
                emit(line2)

                yield()
            }
        }
            //.buffer(UNLIMITED, BufferOverflow.DROP_OLDEST)
            .shareIn(
                this,
                SharingStarted.Lazily,
                50000,
                //10000
            )

        val st = flow<String> {
            emit("norse")
            delay(10000)

            emit("upwork")
            delay(10000)

            emit("abc")
            delay(10000)

            emit("11111")
            delay(10000)
        }.onEach {
            println("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ ------------------------------------- $it \n")
        }

        val s = sequence<String> {
            yield("norse")
            //delay(10000)
        }

        val a2 = async(Dispatchers.Default) {
            var a = 25;

            while (true) {
                var key = wgetch(stdscr);
                //if(key=='w'.code) { y_offset--; }
//        if(key=='s') { y_offset++; }

                //var input: CValuesRef<ByteVar> = CValuesRef<>()
                //wgetstr(stdscr, input)


                if (key == 'z'.code) {

                    val terms = flowOf("norse")//, "upwork", "1111", "abc")

                    terms
                        //.onEach { delay(10000) }
                        .flatMapLatest {
                            qq -> lg.filter { it.contains(qq) }
                        }
                        .withIndex()
                        .onEach {
                            if (it.value != null) {
                                println("${it.index} ${it.value} \r\n")
                                //waddstr(fp, "${it.index} ${it.value}\n")

                                //prefresh(fp, it.index, 0, 5, 0, 40, 130)
                            }
                            //
                        }
                        .launchIn(this)

                    val fl = flowOf("norse")//.delayEach()//.onEach { delay(10000) }

                    /*val lll: Flow<String?> = combine(lg, fl) { l, r ->

                        if (l.contains(r)) {
                            l
                        } else {
                            null
                        }
                    }

                    lll
                        .withIndex()
                        .onEach {
                            if (it.value != null) {
                                println("${it.index} ${it.value} \r\n")
                                //waddstr(fp, "${it.index} ${it.value}\n")

                                //prefresh(fp, it.index, 0, 5, 0, 40, 130)
                            }
                            //
                        }
                        .launchIn(this)*/
                }


                if (key == 'w'.code) {
                    a--; }
                if (key == 's'.code) {
                    a++; }
                if (key == 'q'.code) {
                    delwin(fp); exit(0)
                }

                //prefresh(fp,a, 0, 10,0, 40,120);

                //mvprintw(0, 0, "Input: $key");

                wprintw(stdscr, "Key: $key")
                wrefresh(stdscr)
                clrtoeol();

                sleep(1U)
            }
        }

        val buffer = Buffer()
        val b = buffer.peek()


            //.filter { it.contains("GL") }
            //.filter { it.contains("A") }
/*            .filter { it.contains("norse") }
            .withIndex()
            .onEach {
                waddstr(fp, "${it.index} ${it.value}\n")

                prefresh(fp, it.index, 0, 5, 0, 40, 130)
                //println("${it.index} ${it.value}\r")
            }
            .launchIn(this)*/

/*        async(Dispatchers.Default) {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader: com.kgit2.io.Reader? = child.getChildStdout()

            val cf: Flow<Int> = flowOf(1)

            coroutineScope {

            }

            async {
                //(1..30).forEach {
                var i = 0;
                while (!b.exhausted()) {

                }
                while (true) {
                    val line2 = b.readUtf8Line()

                    if (line2 != null) {
                        waddstr(fp, "$i $line2")
                        //println("$i $line2\n")

                        prefresh(fp, i, 0, 10, 0, 40, 120)
                        i++
                    }

                    // try yield()
                    sleep(1U)
                }
            }

            var i = 0

            while (true) {
                val line = stdoutReader!!.readLine() ?: break

                buffer.writeUtf8(line)


                *//*val line2 = b.readUtf8Line() ?: break

                waddstr(fp, "$i $line2\n")
                println("$i")

                prefresh(fp, i, 0, 10,0, 40,120)
                i++*//*
            }


            *//*(0..5000).forEach {
                waddstr(fp, "$it lkjhlkjhkjhlk lkhjl jlh kjhljh .......\n")
            }
            (0..5000).forEach {
             //   waddstr(fp, "$it lkjhlkjhkjhlk lkhjl jlh kjhljh .......\n")

                prefresh(fp, it, 0, 10, 0, 40, 120)
                //sleep(1U)
            }*//*
        }*/

        /*async(Dispatchers.Default) {
            var i = 0
            val b = buffer.peek()
            while (true) {


            }
        }*/

        /*val a1 = launch(Dispatchers.Default) {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader: com.kgit2.io.Reader? = child.getChildStdout()

            var i = 0
            while (true) {
                val line = stdoutReader!!.readLine()

                waddstr(fp, "$i $line\n")

                prefresh(fp, i, 0, 10,0, 40,120)
                i++
            }

            *//*(0..5000).forEach {
                val line = stdoutReader!!.readLine()

                waddstr(fp, "$line\n")

                prefresh(fp, it, 0, 10,0, 40,120)
            }*//*
        }*/
        //defer {  }


        //awaitAll(a2, a1)
    }

    //sleep(1000U)

    //getstr(aaa)
    //mvwprintw(stdscr, 0, 0, "Key: $key")
    //wrefresh(stdscr)


    /*(1..100).forEach {
        sleep(1U)
        prefresh(fp,25 + it, 0, 10,0, 40,120);
    }

    sleep(1000U)
//    exit(0)
    while (true) {
        //val ch = getwchar()

        val ll = getch()

        //prefresh(fp,26, 0, 10,0, 40,120);
        wscrl(fp, -1)
    }*/

    //sleep(1000U)
}
