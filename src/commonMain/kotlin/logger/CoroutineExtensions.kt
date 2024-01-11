package logger

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext

@OptIn(ExperimentalStdlibApi::class)
suspend fun context() = "[${currentCoroutineContext()[CoroutineDispatcher]}]"
