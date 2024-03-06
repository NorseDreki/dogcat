package com.norsedreki.dogcat.app.ui

interface HasLifecycle {
    suspend fun start()

    suspend fun stop()
}