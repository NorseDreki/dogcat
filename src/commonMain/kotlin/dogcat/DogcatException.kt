package dogcat

class DogcatException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)