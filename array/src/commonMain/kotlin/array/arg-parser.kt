package array.options

class InvalidOption(name: String) : Exception("Invalid arg: ${name}")

class Option(val name: String, val requireArg: Boolean = false, val description: String? = null)
class OptionResult(val option: Option, val arg: String?)

private val LONG_OPTION_ARG_PATTERN = "^--([a-zA-Z0-9-]+)=(.*)$".toRegex()
private val LONG_OPTION_NO_ARG_PATTERN = "^--([a-zA-Z0-9-]+)$".toRegex()
private val SHORT_OPTION_PATTERN = "^-[a-zA-Z0-9]$".toRegex()

class ArgParser(vararg options: Option) {
    val definedOptions: Map<String, Option>

    init {
        definedOptions = HashMap()
        options.forEach { option ->
            definedOptions[option.name] = option
        }
    }

    fun parse(args: Array<String>): Map<String, String?> {
        val parseResults = HashMap<String, String?>()
        args.forEach { arg ->
            val option = matchOption(arg)
            parseResults[option.option.name] = option.arg
        }
        return parseResults
    }

    fun printHelp() {
        println("Options:")
        definedOptions.keys.sorted().forEach { key ->
            val option = definedOptions[key]!!
            val description = option.description
            val buf = StringBuilder()
            buf.append("  --")
            buf.append(option.name)
            if (description != null) {
                buf.append(" ")
                buf.append(description)
            }
            println(buf.toString())
        }
    }

    private fun matchOption(arg: String): OptionResult {
        val argResult = LONG_OPTION_ARG_PATTERN.matchEntire(arg)
        return if (argResult != null) {
            lookup(arg, argResult.groups.get(1)!!.value, argResult.groups.get(2)!!.value)
        } else {
            val longNoArgResult = LONG_OPTION_NO_ARG_PATTERN.matchEntire(arg)
            if (longNoArgResult != null) {
                lookup(arg, longNoArgResult.groups.get(1)!!.value, null)
            } else {
                throw InvalidOption(arg)
            }
        }
    }

    private fun lookup(originalArg: String, name: String, arg: String?): OptionResult {
        val option = definedOptions[name] ?: throw InvalidOption(originalArg)
        if ((option.requireArg && arg == null) || (!option.requireArg && arg != null)) {
            throw InvalidOption(originalArg)
        }
        return OptionResult(option, arg)
    }
}
