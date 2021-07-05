package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class OuterJoinTest : APLTest() {
    @Test
    fun test1x1() {
        val result = parseAPLExpression("1 2 ∘.× 1 2 3 4")
        assertDimension(dimensionsOfSize(2, 4), result)
        assertArrayContent(arrayOf(1, 2, 3, 4, 2, 4, 6, 8), result)
    }

    @Test
    fun testArrayResult() {
        val result = parseAPLExpression("1 2 ∘.{⍺,⍵} 9 8 7 6")
        assertDimension(dimensionsOfSize(2, 4), result)
        assertPairs(
            result,
            arrayOf(1, 9),
            arrayOf(1, 8),
            arrayOf(1, 7),
            arrayOf(1, 6),
            arrayOf(2, 9),
            arrayOf(2, 8),
            arrayOf(2, 7),
            arrayOf(2, 6))
    }

    @Test
    fun testInnerJoin() {
        val result = parseAPLExpression("100 200 300+.×5 6 7")
        assertSimpleNumber(3800, result)
    }

    @Test
    fun testInnerJoinInvalidDimensions() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("100 200 300+.×5 6 7 8").collapse()
        }
    }

    @Test
    fun testInnerJoinLarge() {
        val result = parseAPLExpression("(2 3 1 2 ⍴ ⍳24) +.× 2 4 1 3 7 ⍴ ⍳1000")
        assertDimension(dimensionsOfSize(2, 3, 1, 4, 1, 3, 7), result)
        assertArrayContent(
            arrayOf(
                84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100,
                101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114,
                115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128,
                129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142,
                143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156,
                157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 252, 257, 262,
                267, 272, 277, 282, 287, 292, 297, 302, 307, 312, 317, 322, 327, 332,
                337, 342, 347, 352, 357, 362, 367, 372, 377, 382, 387, 392, 397, 402,
                407, 412, 417, 422, 427, 432, 437, 442, 447, 452, 457, 462, 467, 472,
                477, 482, 487, 492, 497, 502, 507, 512, 517, 522, 527, 532, 537, 542,
                547, 552, 557, 562, 567, 572, 577, 582, 587, 592, 597, 602, 607, 612,
                617, 622, 627, 632, 637, 642, 647, 652, 657, 662, 667, 420, 429, 438,
                447, 456, 465, 474, 483, 492, 501, 510, 519, 528, 537, 546, 555, 564,
                573, 582, 591, 600, 609, 618, 627, 636, 645, 654, 663, 672, 681, 690,
                699, 708, 717, 726, 735, 744, 753, 762, 771, 780, 789, 798, 807, 816,
                825, 834, 843, 852, 861, 870, 879, 888, 897, 906, 915, 924, 933, 942,
                951, 960, 969, 978, 987, 996, 1005, 1014, 1023, 1032, 1041, 1050,
                1059, 1068, 1077, 1086, 1095, 1104, 1113, 1122, 1131, 1140, 1149,
                1158, 1167, 588, 601, 614, 627, 640, 653, 666, 679, 692, 705, 718,
                731, 744, 757, 770, 783, 796, 809, 822, 835, 848, 861, 874, 887, 900,
                913, 926, 939, 952, 965, 978, 991, 1004, 1017, 1030, 1043, 1056, 1069,
                1082, 1095, 1108, 1121, 1134, 1147, 1160, 1173, 1186, 1199, 1212,
                1225, 1238, 1251, 1264, 1277, 1290, 1303, 1316, 1329, 1342, 1355,
                1368, 1381, 1394, 1407, 1420, 1433, 1446, 1459, 1472, 1485, 1498,
                1511, 1524, 1537, 1550, 1563, 1576, 1589, 1602, 1615, 1628, 1641,
                1654, 1667, 756, 773, 790, 807, 824, 841, 858, 875, 892, 909, 926,
                943, 960, 977, 994, 1011, 1028, 1045, 1062, 1079, 1096, 1113, 1130,
                1147, 1164, 1181, 1198, 1215, 1232, 1249, 1266, 1283, 1300, 1317,
                1334, 1351, 1368, 1385, 1402, 1419, 1436, 1453, 1470, 1487, 1504,
                1521, 1538, 1555, 1572, 1589, 1606, 1623, 1640, 1657, 1674, 1691,
                1708, 1725, 1742, 1759, 1776, 1793, 1810, 1827, 1844, 1861, 1878,
                1895, 1912, 1929, 1946, 1963, 1980, 1997, 2014, 2031, 2048, 2065,
                2082, 2099, 2116, 2133, 2150, 2167, 924, 945, 966, 987, 1008, 1029,
                1050, 1071, 1092, 1113, 1134, 1155, 1176, 1197, 1218, 1239, 1260,
                1281, 1302, 1323, 1344, 1365, 1386, 1407, 1428, 1449, 1470, 1491,
                1512, 1533, 1554, 1575, 1596, 1617, 1638, 1659, 1680, 1701, 1722,
                1743, 1764, 1785, 1806, 1827, 1848, 1869, 1890, 1911, 1932, 1953,
                1974, 1995, 2016, 2037, 2058, 2079, 2100, 2121, 2142, 2163, 2184,
                2205, 2226, 2247, 2268, 2289, 2310, 2331, 2352, 2373, 2394, 2415,
                2436, 2457, 2478, 2499, 2520, 2541, 2562, 2583, 2604, 2625, 2646, 2667
            ), result)
    }

    @Test
    fun firstAxisSingleDimension() {
        val result = parseAPLExpression("(10 ⍴ ⍳1000) +.× 10 3 3 ⍴ ⍳1000")
        assertDimension(dimensionsOfSize(3, 3), result)
        assertArrayContent(
            arrayOf(
                2565, 2610, 2655, 2700, 2745, 2790, 2835, 2880, 2925
            ), result)
    }

    @Test
    fun secondAxisSingleDimension() {
        val result = parseAPLExpression("(2 3 1 30 ⍴ ⍳24) +.× 30 ⍴ ⍳1000")
        assertDimension(dimensionsOfSize(2, 3, 1), result)
        assertArrayContent(arrayOf(4739, 4397, 4919, 6305, 4739, 4397), result)
    }

    @Test
    fun onlySingleDimensionalArguments() {
        val result = parseAPLExpression("(1+⍳8) +.× (10+⍳8)")
        assertSimpleNumber(528, result)
    }

    @Test
    fun singleNumberWithSimpleArrayLeftArg() {
        parseAPLExpression("10 +.× ⍳8").let { result ->
            assertSimpleNumber(280, result)
        }
    }

    @Test
    fun singleNumberWithSimpleArrayRightArg() {
        parseAPLExpression("(⍳8) +.× ⍳8").let { result ->
            assertSimpleNumber(140, result)
        }
    }

    @Test
    fun singleNumberWithMultiDimensionalArrayLeftArg() {
        parseAPLExpression("10 +.× 3 4 5 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(4, 5), result)
            assertArrayContent(
                arrayOf(
                    600, 630, 660, 690, 720,
                    750, 780, 810, 840, 870,
                    900, 930, 960, 990, 1020,
                    1050, 1080, 1110, 1140, 1170
                ), result)
        }
    }

    @Test
    fun singleNumberWithMultiDimensionalArrayRightArg() {
        parseAPLExpression("(3 4 5 ⍴ ⍳1000) +.× 100").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(
                arrayOf(
                    1000, 3500, 6000, 8500,
                    11000, 13500, 16000, 18500,
                    21000, 23500, 26000, 28500
                ), result)
        }
    }

    @Test
    fun innerJoinWithLength1Arg() {
        parseAPLExpression("(4 1 ⍴ 20 30 40 50) +.× (1 1 ⍴ 9)").let { result ->
            assertDimension(dimensionsOfSize(4, 1), result)
            assertArrayContent(arrayOf(180, 270, 360, 450), result)
        }
    }

    @Test
    fun outerJoinWithScalarRightArg() {
        parseAPLExpression("(15+⍳4)∘.+10").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(25, 26, 27, 28), result)
        }
    }

    @Test
    fun outerJoinScalarRightArgAndArray() {
        parseAPLExpression("(3 5 ⍴ (10+⍳20))∘.+110").let { result ->
            assertDimension(dimensionsOfSize(3, 5), result)
            assertArrayContent(arrayOf(120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134), result)
        }
    }

    @Test
    fun outerJoinWithScalarLeftArg() {
        parseAPLExpression("10∘.+(15+⍳4)").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(25, 26, 27, 28), result)
        }
    }

    @Test
    fun outerJoinScalarLeftArgAndArray() {
        parseAPLExpression("10∘.+3 5 ⍴ (10+⍳20)").let { result ->
            assertDimension(dimensionsOfSize(3, 5), result)
            assertArrayContent(arrayOf(20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34), result)
        }
    }
}
