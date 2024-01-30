package ui

interface HasLifecycle {
    suspend fun start()

    suspend fun stop()
}