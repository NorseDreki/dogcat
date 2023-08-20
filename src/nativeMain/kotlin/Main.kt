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

val greenColor = "\u001b[31;1;4m"
val reset = "\u001b[0m" // to reset color to the default
val name = greenColor + "Alex" + reset // Add green only to Alex
val sm = "\u001b[263a]"

/*val m = r2.matchEntire(line)
if (m != null) {
    //   println("11111 $line")
    val (level, tag, owner, message) = m.destructured

    //println(line)
}*/

/*
private val autofillSuggestions = currentSearchQuery.debounce(250)
    .distinctUntilChanged()
    .filter { it.isNotBlank() }
    .mapLatest { query ->
        isLoadingAutofillSuggestions.value = true
        locationServicesRepository.fetchSuggestedPlacesForQuery(query)
            .also { isLoadingAutofillSuggestions.value = false }
    }
*/

@OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
fun main(): Unit = memScoped {
    setlocale(LC_CTYPE, "")
    //setlocale(LC_ALL, "en_US.UTF-8");
    initscr();
    intrflush(stdscr, false);
    savetty();
    noecho();//disable auto-echoing
    /*You've set nodelay so getch will return immediately with ERR if there's no data ready from the terminal. That's why getch is returning -1 (ERR). You haven't set cbreak or raw to disable terminal buffering, so you're still getting that -- no data will come from the terminal until Enter is hit.

    So add a call to cbreak() at the start (just before or after the call to nodelay()) and it should work as you expect.
    Applications will also commonly need to react to keys instantly, without requiring the Enter key to be pressed; this is called cbreak mode, as opposed to the usual buffered input mode.
    */
    //cbreak();//making getch() work without a buffer I.E. raw characters

    //Terminals usually return special keys, such as the cursor keys or navigation keys such as Page Up and Home, as a multibyte escape sequence. While you could write your application to expect such sequences and process them accordingly, curses can do it for you, returning a special value such as curses.KEY_LEFT. To get curses to do the job, you’ll have to enable keypad mode.
    keypad(stdscr, true);//allows use of special keys, namely the arrow keys
    clear();    // empty the screen
    //timeout(0); // reads do not block
    //nodelay(sdtscr)
    //curs_set(0)
    //halfdelay(2)

    /*val argc = alloc<IntVar>()
    argc.value = args.size
    val argv = alloc<CPointerVar<CPointerVar<ByteVar>>>()
    argv.value = args.map { it.cstr.ptr }.toCValues().ptr
    gtk_init(argc.ptr, argv.ptr)*/

/*
    alloc()

    val str: StructType = alloc<StructType>()
    val strPtr: CPointer<StructType> = str.ptr

    val i = alloc<IntVar>()
    i.value = 5
    val p = i.ptr

    val stringBuilder = StringBuilder()
    val stableRef = StableRef.create(stringBuilder)
    val cPtr = stableRef.asCPointer()
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, cPtr)

    var ggg = CValuesRef<UByteVar>()

    val data = null
    var dataRef: CValuesRef<ByteVar> = CPointer(t)//cValuesOf(t)

    wgetstr()
*/

    //nonl() as Unit /* tell curses not to do NL->CR/NL on output */
    //cbreak() as Unit /* take input chars one at a time, no wait for \n */
    //noecho() as Unit /* don't echo input */
    //if (!single_step) nodelay(stdscr, TRUE)
    //idlok(stdscr, TRUE) /* allow use of insert/delete line */

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    val fp = newpad(32767, sx)
    scrollok(fp, true)
    //keypad(fp, true)

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
            )

        val a2 = async(Dispatchers.Default) {
            var a = 25;

            var j: Job? = null

            while (true) {
                var key = wgetch(stdscr);

                when (key) {
                    'f'.code -> {
                        mvwprintw(stdscr, 0, 0, "$sx:$sy")
                        clrtoeol()
                        echo()

                        val bytePtr = allocArray<ByteVar>(200)

                        getnstr(bytePtr, 200)

                        noecho()

                        wclear(fp)
                        //clear()
                        yield()
                        j?.cancelAndJoin()

                        j = lg
                            .filter { it.contains(bytePtr.toKString()) }
                            //.take(10)
                            //.take(10)
                            .withIndex()
                            .onEach {
                                //if (it.value != null) {
                                //println("${it.index} ${it.value} \r\n")
                                waddstr(fp, "${it.index} ${it.value}\n")

                                prefresh(fp, it.index, 0, 3, 0, sy-1, sx)

                                a = it.index

                                yield()
                                //}
                                //
                            }
                            .launchIn(this)
                        yield()
                    }
                    'q'.code -> {
                        delwin(fp); exit(0)
                    }
                    'a'.code -> {
                        a = 0
                        prefresh(fp, 0, 0, 3,0, sy-1, sx);
                    }
                    'z'.code -> {
                        /*a = 0
                        prefresh(fp, 0, 0, 3,0, sy-1, sx);*/
                    }
                    'w'.code -> {
                        a--
                        prefresh(fp, a, 0, 3,0, sy-1, sx);
                    }
                    's'.code -> {
                        a++
                        prefresh(fp, a, 0, 3,0, sy-1, sx);
                    }
                    'd'.code -> {
                        a += sy-1-3
                        prefresh(fp, a, 0, 3,0, sy-1, sx);
                    }
                    'e'.code -> {
                        a -= sy-1-3
                        prefresh(fp, a, 0, 3,0, sy-1, sx);
                    }
                }
            }
        }
    }
}
