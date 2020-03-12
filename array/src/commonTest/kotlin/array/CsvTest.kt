package array

import array.csv.CsvParseException
import array.csv.readCsv
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CsvTest : APLTest() {
    @Test
    fun simpleCsv() {
        val content = """
            |0,1,2,3
            |10,11,12,13
            |20,21,22,23
            |30,31,32,33
            |40,41,42,43
        """.trimMargin()
        val input = StringCharacterProvider(content)
        val result = readCsv(input)
        assertDimension(dimensionsOfSize(5, 4), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 10, 11, 12, 13, 20, 21, 22, 23, 30, 31, 32, 33, 40, 41, 42, 43), result)
    }

    @Test
    fun stringCsv() {
        val content = """
            |0,1
            |"test",a b
            |"foo , bar","1234"
        """.trimMargin()
        val input = StringCharacterProvider(content)
        val result = readCsv(input)
        assertDimension(dimensionsOfSize(3, 2), result)
        assertSimpleNumber(0, result.valueAt(0))
        assertSimpleNumber(1, result.valueAt(1))
        assertString("test", result.valueAt(2))
        assertString("a b", result.valueAt(3))
        assertString("foo , bar", result.valueAt(4))
        assertString("1234", result.valueAt(5))
    }

    @Test
    fun stringEscape() {
        val content = """
            |"\"","\"foo","a\""
            |"test\"middle","a\"b\"cd"
        """.trimMargin()
        val input = StringCharacterProvider(content)
        val result = readCsv(input)
        assertDimension(dimensionsOfSize(2, 3), result)

    }

    @Test
    fun csvWithWhitespace() {
        val content = """
            |  foo ,   a  bc   ,2
            |" a   xy  " ,  10 , "  " 
            |"   foo , bar ","    1234  ","  "
            |a , b  ,"dw"
        """.trimMargin()
        val input = StringCharacterProvider(content)
        val result = readCsv(input)
        assertDimension(dimensionsOfSize(4, 3), result)
        assertString("foo", result.valueAt(0))
        assertString("a  bc", result.valueAt(1))
        assertSimpleNumber(2, result.valueAt(2))

        assertString(" a   xy  ", result.valueAt(3))
        assertSimpleNumber(10, result.valueAt(4))
        assertString("  ", result.valueAt(5))

        assertString("   foo , bar ", result.valueAt(6))
        assertString("    1234  ", result.valueAt(7))
        assertString("  ", result.valueAt(8))

        assertString("a", result.valueAt(9))
        assertString("b", result.valueAt(10))
        assertString("dw", result.valueAt(11))
    }

    @Test
    fun csvWithNull() {
        val content = """
            |1,2
            |3,4,5,6,7,8
            |9,10
            |11,12,13,14,15,16
        """.trimMargin()
        val input = StringCharacterProvider(content)
        val result = readCsv(input)
        assertDimension(dimensionsOfSize(4, 6), result)
        assertSimpleNumber(1, result.valueAt(0))
        assertSimpleNumber(2, result.valueAt(1))
        assertAPLNull(result.valueAt(2))
        assertAPLNull(result.valueAt(3))
        assertAPLNull(result.valueAt(4))
        assertAPLNull(result.valueAt(5))

        assertSimpleNumber(3, result.valueAt(6))
        assertSimpleNumber(4, result.valueAt(7))
        assertSimpleNumber(5, result.valueAt(8))
        assertSimpleNumber(6, result.valueAt(9))
        assertSimpleNumber(7, result.valueAt(10))
        assertSimpleNumber(8, result.valueAt(11))

        assertSimpleNumber(9, result.valueAt(12))
        assertSimpleNumber(10, result.valueAt(13))
        assertAPLNull(result.valueAt(14))
        assertAPLNull(result.valueAt(15))
        assertAPLNull(result.valueAt(16))
        assertAPLNull(result.valueAt(17))

        assertSimpleNumber(11, result.valueAt(18))
        assertSimpleNumber(12, result.valueAt(19))
        assertSimpleNumber(13, result.valueAt(20))
        assertSimpleNumber(14, result.valueAt(21))
        assertSimpleNumber(15, result.valueAt(22))
        assertSimpleNumber(16, result.valueAt(23))
    }

    @Test
    fun emptyFieldTest() {
        val content = """
            |1,,2
            |,  ,3
            |,,
            |20,21,22
        """.trimMargin()
        val input = StringCharacterProvider(content)
        val result = readCsv(input)
        assertDimension(dimensionsOfSize(4, 3), result)
        assertSimpleNumber(1, result.valueAt(0))
        assertString("", result.valueAt(1))
        assertSimpleNumber(2, result.valueAt(2))

        assertString("", result.valueAt(3))
        assertString("", result.valueAt(4))
        assertSimpleNumber(3, result.valueAt(5))

        assertString("", result.valueAt(6))
        assertString("", result.valueAt(7))
        assertString("", result.valueAt(8))

        assertSimpleNumber(20, result.valueAt(9))
        assertSimpleNumber(21, result.valueAt(10))
        assertSimpleNumber(22, result.valueAt(11))
    }

    @Test
    fun incompleteString() {
        val content = """
            |a,"foo
        """.trimIndent()
        val input = StringCharacterProvider(content)
        assertFailsWith<CsvParseException> {
            readCsv(input)
        }
    }

    @Test
    fun floatCells() {
        val content = """
            |1.2,.3,4.
            |.,1e2,1.e
        """.trimMargin()
        val input = StringCharacterProvider(content)
        val result = readCsv(input)
        assertDimension(dimensionsOfSize(2, 3), result)
        assertSimpleDouble(Pair(1.19999, 1.20001), result.valueAt(0))
        assertSimpleDouble(Pair(0.29999, 0.30001), result.valueAt(1))
        assertSimpleDouble(Pair(3.99999, 4.00001), result.valueAt(2))
        assertString(".", result.valueAt(3))
        assertString("1e2", result.valueAt(4))
        assertString("1.e", result.valueAt(5))
    }

    @Test
    fun emptyInput() {
        val content = ""
        val input = StringCharacterProvider(content)
        val result = readCsv(input)
        assertDimension(dimensionsOfSize(0, 0), result)
    }

    @Test
    fun emptyLines() {
        val content = """
            |1,2,3,4
            |
            |5,6,7,8
            |
            |
            |9,10,11,12
            |13,14,15,16
            |17,18,19,20
            |21,22,23,24
        """.trimMargin()
        val input = StringCharacterProvider(content)
        val result = readCsv(input)
        assertDimension(dimensionsOfSize(6, 4), result)
        assertArrayContent(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24), result)
    }
}
