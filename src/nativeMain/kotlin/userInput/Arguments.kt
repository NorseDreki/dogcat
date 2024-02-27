package userInput

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.optional

object Arguments {

    private val parser = ArgParser("dogcat")

    val packageName by parser.argument(ArgType.String, "package name", "description for p n").optional()
    val current by parser.option(ArgType.Boolean, shortName = "c", description = "Filter by currently running program")
    val lineNumbers by parser.option(ArgType.Boolean, shortName = "ln", description = "Show line numbers")

    fun validate(args: Array<String>) {
        parser.parse(args)
        if (packageName != null && current != null) {
            //can't have both at the same time
        }
    }
}
