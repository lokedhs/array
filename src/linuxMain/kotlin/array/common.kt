package array

class IncompatibleTypeException(message: String) : Exception(message)
class APLIndexOutOfBoundsException(message: String) : Exception(message)

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
            unless(a.size == b.size) {
                return false
            }
            val i1 = a.iterator()
            val i2 = b.iterator()
            while(true) {
                val n1 = i1.hasNext()
                val n2 = i2.hasNext()
                if (!n1 && !n2) {
                    return true
                } else if ((n1 && !n2) || (!n1 && n2)) {
                    return false
                } else if (i1.next() != i2.next()) {
                    return false
                }
            }
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
