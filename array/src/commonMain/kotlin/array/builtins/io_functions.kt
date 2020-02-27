package array.builtins

import array.APLValue
import array.NoAxisAPLFunction
import array.RuntimeContext
import array.csv.readCsv
import array.readFile

class PrintAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        println(a.formatted())
        return a
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        TODO("not implemented")
    }
}

class ReadCSVFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        // filename is hardcoded at the moment since string functions are not working
        val source = readFile("/tmp/foo")
        try {
            return readCsv(source)
        } finally {
            source.close()
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        TODO("not implemented")
    }
}
