package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TakeTest : APLTest() {
    @Test
    fun testDropSimple() {
        val result = parseAPLExpression("↓1 2 3 4")
        assertDimension(dimensionsOfSize(3), result)
        assertArrayContent(arrayOf(2, 3, 4), result)
    }

    @Test
    fun testDropFunctionResult() {
        val result = parseAPLExpression("↓10 + 1 2 3 4")
        assertDimension(dimensionsOfSize(3), result)
        assertArrayContent(arrayOf(12, 13, 14), result)
    }

    @Test
    fun testDropFromArray1() {
        val result = parseAPLExpression("↓,1")
        assertDimension(dimensionsOfSize(0), result)
        assertEquals(0, result.size)
    }

    @Test
    fun testTakeSimple() {
        val result = parseAPLExpression("↑1 2 3 4")
        assertTrue(result.isScalar())
        assertEquals(1L, result.ensureNumber().asLong())
    }

    @Test
    fun testTakeFromArray1() {
        val result = parseAPLExpression("↑,1")
        assertTrue(result.isScalar())
        assertEquals(1L, result.ensureNumber().asLong())
    }

    @Test
    fun takeSingleDimension() {
        parseAPLExpression("3 ↑ 10 11 12 13 14 15 16 17").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(10, 11, 12), result)
        }
    }

    @Test
    fun takeMultiDimension() {
        parseAPLExpression("2 3 ↑ 10 15 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(arrayOf(0, 1, 2, 15, 16, 17), result)
        }
    }

    @Test
    fun dropSingleDimension() {
        parseAPLExpression("2 ↓ 100 200 300 400 500 600 700 800").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(300, 400, 500, 600, 700, 800), result)
        }
    }

    @Test
    fun dropMultiDimension() {
        parseAPLExpression("20 34 ↓ 30 40 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(10, 6), result)
            assertArrayContent(arrayOf(
                34, 35, 36, 37, 38, 39, 74, 75, 76, 77, 78, 79, 14, 15, 16, 17, 18,
                19, 54, 55, 56, 57, 58, 59, 94, 95, 96, 97, 98, 99, 34, 35, 36, 37,
                38, 39, 74, 75, 76, 77, 78, 79, 14, 15, 16, 17, 18, 19, 54, 55, 56,
                57, 58, 59, 94, 95, 96, 97, 98, 99
            ), result)
        }
    }

    @Test
    fun takeWithNegativeArg() {
        parseAPLExpression("¯2 ↑ 100 200 300 400 500 600 700 800 900 1000 1100, 1200").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1100, 1200), result)
        }
    }

    @Test
    fun takeMultiDimensionalWithNegativeArg() {
        parseAPLExpression("¯17 ¯20 ↑ 30 40 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(17, 20), result)
            assertArrayContent(arrayOf(
                540, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553,
                554, 555, 556, 557, 558, 559, 580, 581, 582, 583, 584, 585, 586, 587,
                588, 589, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599, 620, 621,
                622, 623, 624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635,
                636, 637, 638, 639, 660, 661, 662, 663, 664, 665, 666, 667, 668, 669,
                670, 671, 672, 673, 674, 675, 676, 677, 678, 679, 700, 701, 702, 703,
                704, 705, 706, 707, 708, 709, 710, 711, 712, 713, 714, 715, 716, 717,
                718, 719, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751,
                752, 753, 754, 755, 756, 757, 758, 759, 780, 781, 782, 783, 784, 785,
                786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799,
                820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833,
                834, 835, 836, 837, 838, 839, 860, 861, 862, 863, 864, 865, 866, 867,
                868, 869, 870, 871, 872, 873, 874, 875, 876, 877, 878, 879, 900, 901,
                902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 912, 913, 914, 915,
                916, 917, 918, 919, 940, 941, 942, 943, 944, 945, 946, 947, 948, 949,
                950, 951, 952, 953, 954, 955, 956, 957, 958, 959, 980, 981, 982, 983,
                984, 985, 986, 987, 988, 989, 990, 991, 992, 993, 994, 995, 996, 997,
                998, 999, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
                35, 36, 37, 38, 39, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71,
                72, 73, 74, 75, 76, 77, 78, 79, 100, 101, 102, 103, 104, 105, 106,
                107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 140,
                141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154,
                155, 156, 157, 158, 159, 180, 181, 182, 183, 184, 185, 186, 187, 188,
                189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199
            ), result)
        }
    }

    @Test
    fun dropWithNegativeArg() {
        parseAPLExpression("¯2 ↓ 1 2 3 4 5 6").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 3, 4), result)
        }
    }

    @Test
    fun dropMultiDimensionalWithNegativeArg() {
        parseAPLExpression("¯26 ¯32 ↓ 30 40 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(4, 8), result)
            assertArrayContent(arrayOf(
                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                40,
                41,
                42,
                43,
                44,
                45,
                46,
                47,
                80,
                81,
                82,
                83,
                84,
                85,
                86,
                87,
                120,
                121,
                122,
                123,
                124,
                125,
                126,
                127
            ), result)
        }
    }
}
