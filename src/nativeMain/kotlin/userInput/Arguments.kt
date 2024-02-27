package userInput

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.optional

class Arguments(private val parser: ArgParser) {

    class ValidationException(override val message: String) : RuntimeException(message)

    val packageName by parser.argument(
        ArgType.String,
        "package name",
        "Show logs for a particular application specified by this package name. " +
                "Example: 'com.google.android.apps.messaging'."
    ).optional()

    val current by parser.option(
        ArgType.Boolean,
        shortName = "c",
        description = "Show logs for an application currently running in foreground"
    )

    val lineNumbers by parser.option(
        ArgType.Boolean,
        shortName = "ln",
        description = "Show line numbers for log lines, embedded into message body"
    )

    fun validate(args: Array<String>) {
        parser.parse(args)
        if (packageName != null && current != null) {
            throw ValidationException("'Package name' and '--current' arguments are mutually exclusive and " +
                    "can't be used at the same time.")
        }
    }
}
