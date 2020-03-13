package array.builtins

import array.*
import array.csv.readCsv

class PrintAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        println(a.formatted())
        return a
    }
}

class ReadCSVFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        val source = openCharFile(arrayAsStringValue(a))
        try {
            return readCsv(source)
        } finally {
            source.close()
        }
    }
}
