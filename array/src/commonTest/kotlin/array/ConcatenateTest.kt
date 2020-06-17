package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ConcatenateTest : APLTest() {
    @Test
    fun simpleConcatenate() {
        parseAPLExpression("1 2 3 , 4 5").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 2, 3, 4, 5), result)
        }
    }

    @Test
    fun scalarWithVector() {
        parseAPLExpression("1 , 2 3 4 5").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 2, 3, 4, 5), result)
        }
    }

    @Test
    fun vectorWithScalar() {
        parseAPLExpression("1 2 3 4 , 5").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 2, 3, 4, 5), result)
        }
    }

    @Test
    fun scalarWithScalar() {
        parseAPLExpression("1 , 2").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 2), result)
        }
    }

    @Test
    fun twoDimensionalConcat() {
        parseAPLExpression("(4 5 ⍴ ⍳20) , 1000+⍳4").let { result ->
            assertDimension(dimensionsOfSize(4, 6), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 1000, 5, 6, 7, 8, 9, 1001, 10, 11, 12, 13, 14, 1002,
                    15, 16, 17, 18, 19, 1003
                ), result)
        }
    }

    @Test
    fun twoDimensionalConcatWithExplicitAxis() {
        parseAPLExpression("(4 5 ⍴ ⍳20) ,[1] 1000+⍳4").let { result ->
            assertDimension(dimensionsOfSize(4, 6), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 1000, 5, 6, 7, 8, 9, 1001, 10, 11, 12, 13, 14, 1002,
                    15, 16, 17, 18, 19, 1003
                ), result)
        }
    }

    @Test
    fun twoDimensionalFirstAxis() {
        parseAPLExpression("(4 5 ⍴ ⍳20) ,[0] 1000+⍳5").let { result ->
            assertDimension(dimensionsOfSize(5, 5), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                    1000, 1001, 1002, 1003, 1004
                ), result)
        }
    }

    @Test
    fun mismatchedDimensions() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(3 4 ⍴ ⍳12) , 1 2")
        }
    }

    @Test
    fun concatenateScalar() {
        parseAPLExpression("(5 6 ⍴ ⍳30) , 1234").let { result ->
            assertDimension(dimensionsOfSize(5, 7), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 1234, 6, 7, 8, 9, 10, 11, 1234, 12, 13, 14, 15, 16,
                    17, 1234, 18, 19, 20, 21, 22, 23, 1234, 24, 25, 26, 27, 28, 29, 1234
                ), result)
        }
    }

    @Test
    fun concatenateWithNewDimension0() {
        parseAPLExpression("(10 11 ⍴ ⍳110) ,[0.5] 10 11 ⍴ 10000+⍳110").let { result ->
            assertDimension(dimensionsOfSize(10, 2, 11), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10000, 10001, 10002, 10003, 10004,
                    10005, 10006, 10007, 10008, 10009, 10010, 11, 12, 13, 14, 15, 16, 17,
                    18, 19, 20, 21, 10011, 10012, 10013, 10014, 10015, 10016, 10017,
                    10018, 10019, 10020, 10021, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
                    32, 10022, 10023, 10024, 10025, 10026, 10027, 10028, 10029, 10030,
                    10031, 10032, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 10033,
                    10034, 10035, 10036, 10037, 10038, 10039, 10040, 10041, 10042, 10043,
                    44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 10044, 10045, 10046,
                    10047, 10048, 10049, 10050, 10051, 10052, 10053, 10054, 55, 56, 57,
                    58, 59, 60, 61, 62, 63, 64, 65, 10055, 10056, 10057, 10058, 10059,
                    10060, 10061, 10062, 10063, 10064, 10065, 66, 67, 68, 69, 70, 71, 72,
                    73, 74, 75, 76, 10066, 10067, 10068, 10069, 10070, 10071, 10072,
                    10073, 10074, 10075, 10076, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86,
                    87, 10077, 10078, 10079, 10080, 10081, 10082, 10083, 10084, 10085,
                    10086, 10087, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 10088,
                    10089, 10090, 10091, 10092, 10093, 10094, 10095, 10096, 10097, 10098,
                    99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 10099, 10100,
                    10101, 10102, 10103, 10104, 10105, 10106, 10107, 10108, 10109
                ), result)
        }
    }

    @Test
    fun concatenateWithNewDimension1() {
        parseAPLExpression("(10 11 ⍴ ⍳110) ,[1.5] 10 11 ⍴ 10000+⍳110").let { result ->
            assertDimension(dimensionsOfSize(10, 11, 2), result)
            assertArrayContent(
                arrayOf(
                    0, 10000, 1, 10001, 2, 10002, 3, 10003, 4, 10004, 5, 10005, 6, 10006,
                    7, 10007, 8, 10008, 9, 10009, 10, 10010, 11, 10011, 12, 10012, 13,
                    10013, 14, 10014, 15, 10015, 16, 10016, 17, 10017, 18, 10018, 19,
                    10019, 20, 10020, 21, 10021, 22, 10022, 23, 10023, 24, 10024, 25,
                    10025, 26, 10026, 27, 10027, 28, 10028, 29, 10029, 30, 10030, 31,
                    10031, 32, 10032, 33, 10033, 34, 10034, 35, 10035, 36, 10036, 37,
                    10037, 38, 10038, 39, 10039, 40, 10040, 41, 10041, 42, 10042, 43,
                    10043, 44, 10044, 45, 10045, 46, 10046, 47, 10047, 48, 10048, 49,
                    10049, 50, 10050, 51, 10051, 52, 10052, 53, 10053, 54, 10054, 55,
                    10055, 56, 10056, 57, 10057, 58, 10058, 59, 10059, 60, 10060, 61,
                    10061, 62, 10062, 63, 10063, 64, 10064, 65, 10065, 66, 10066, 67,
                    10067, 68, 10068, 69, 10069, 70, 10070, 71, 10071, 72, 10072, 73,
                    10073, 74, 10074, 75, 10075, 76, 10076, 77, 10077, 78, 10078, 79,
                    10079, 80, 10080, 81, 10081, 82, 10082, 83, 10083, 84, 10084, 85,
                    10085, 86, 10086, 87, 10087, 88, 10088, 89, 10089, 90, 10090, 91,
                    10091, 92, 10092, 93, 10093, 94, 10094, 95, 10095, 96, 10096, 97,
                    10097, 98, 10098, 99, 10099, 100, 10100, 101, 10101, 102, 10102, 103,
                    10103, 104, 10104, 105, 10105, 106, 10106, 107, 10107, 108, 10108,
                    109, 10109
                ), result)
        }
    }

    @Test
    fun concatenateWithFirstAxis() {
        parseAPLExpression("(2 3 ⍴ ⍳100) ⍪ (8 3 ⍴ 100+⍳100)").let { result ->
            assertDimension(dimensionsOfSize(10, 3), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
                    110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123
                ), result)
        }
    }

    @Test
    fun concatenateWithFirstAxisAndAxisSpecifier() {
        parseAPLExpression("(3 10 ⍴ ⍳100) ⍪[1] (3 15 ⍴ 100+⍳100)").let { result ->
            assertDimension(dimensionsOfSize(3, 25), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 100, 101, 102, 103, 104, 105, 106, 107,
                    108, 109, 110, 111, 112, 113, 114, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                    19, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127,
                    128, 129, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 130, 131, 132, 133,
                    134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144
                ), result)
        }
    }

}
