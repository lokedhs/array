package array.plainclient

import array.CharacterProvider
import array.Engine
import array.MPFileException
import array.repl.runRepl

class Repl {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runRepl(args, init = ::engineInit)
        }

        private fun engineInit(engine: Engine) {
            engine.standardInput = StandardInputReader()
        }
    }

    class StandardInputReader : CharacterProvider {
        override fun nextCodepoint(): Int? {
            val ch = System.`in`.read()
            return when {
                ch == -1 -> null
                Character.isHighSurrogate(ch.toChar()) -> {
                    val nextChar = System.`in`.read()
                    when {
                        nextChar == -1 -> throw MPFileException("Input terminated in the middle of a surrogate pair")
                        !Character.isLowSurrogate(nextChar.toChar()) -> throw MPFileException("High surrogate not followed by low surrogate")
                        else -> Character.toCodePoint(ch.toChar(), nextChar.toChar())
                    }
                }
                else -> ch
            }
        }

        override fun close() {}
    }
}
