package com.norsedreki.dogcat

class DogcatException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)
