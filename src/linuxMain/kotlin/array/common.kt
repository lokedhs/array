package array

class IncompatibleTypeException(message: String) : Exception(message)
class APLIndexOutOfBoundsException(message: String) : Exception(message)
class IllegalNumberFormat(message: String) : Exception(message)
class UnexpectedSymbol(ch: Char) : Exception("Unexpected symbol: ${ch}")
class UnexpectedToken(token: Token) : Exception("Unexpected token: ${token}")

inline fun unless(cond: Boolean, fn: () -> Unit) {
    if(!cond) {
        fn()
    }
}

class Arrays {
    companion object {
        fun <T> equals(a: Array<T>, b: Array<T>): Boolean {
            if(a === b) {
                return true
            }
            val aLength = a.size
            val bLength = b.size
            unless(aLength == bLength) {
                return false
            }

            for(i in 0 until aLength) {
                if(a[i] != b[i]) {
                    return false;
                }
            }
            return true;
        }

        fun toString(values: Array<*>): String {
            val buf = StringBuilder()
            buf.append("[")
            values.forEach { v ->
                buf.append(" ")
                buf.append(v.toString())
            }
            buf.append(" ]")
            return buf.toString()
        }
    }
}
