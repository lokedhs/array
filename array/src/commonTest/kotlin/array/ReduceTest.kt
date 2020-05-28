package array

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReduceTest : APLTest() {
    @Test
    fun reduceIotaTest() {
        val result = parseAPLExpression("+/⍳1000")
        assertSimpleNumber(499500, result)
    }

    @Test
    fun reduceTestWithFunction() {
        val result = parseAPLExpression("+/⍳1+2")
        assertSimpleNumber(3, result)
    }

    @Test
    fun reduceWithSingleValue() {
        val result = parseAPLExpression("+/,4")
        assertSimpleNumber(4, result)
    }

    @Test
    fun reduceWithScalar() {
        val result = parseAPLExpression("+/4")
        assertSimpleNumber(4, result)
    }

    @Test
    fun reduceWithEmptyArg() {
        reduceTestWithFunctionName("+", 0)
        reduceTestWithFunctionName("-", 0)
        reduceTestWithFunctionName("×", 1)
        reduceTestWithFunctionName("÷", 1)
        reduceTestWithFunctionName("⋆", 1)
        reduceTestWithFunctionName("=", 1)
        reduceTestWithFunctionName("≠", 0)
        reduceTestWithFunctionName("<", 0)
        reduceTestWithFunctionName(">", 0)
        reduceTestWithFunctionName("≤", 1)
        reduceTestWithFunctionName("≥", 1)
    }

    @Test
    fun reduceWithNonScalarCells() {
        val result = parseAPLExpression("+/ (1 2 3 4) (6 7 8 9)")
        assertDimension(emptyDimensions(), result)

        val v = result.valueAt(0)
        assertDimension(dimensionsOfSize(4), v)
        assertArrayContent(arrayOf(7, 9, 11, 13), v)
    }

    @Test
    fun reduceCustomFn() {
        val result = parseAPLExpression("{⍺+⍵+10}/⍳6")
        assertSimpleNumber(65, result)
    }

    @Test
    fun reduceAlongAxis() {
        parseAPLExpression("e←3 4 ⍴ 1 2 3 4 5 6 7 8 9 10 11 12 ◊ +/[0] e").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(15, 18, 21, 24), result)
        }
    }

    @Test
    fun invalidAxisTest() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("+/[2] 2 3 ⍴ ⍳6")
        }
    }

    @Test
    fun multiDimensionalReduce() {
        parseAPLExpression("+/[0] 3 3 4 5 6 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(3, 4, 5, 6), result)
            assertArrayContent(
                arrayOf(
                    1080, 1083, 1086, 1089, 1092, 1095, 1098, 1101, 1104, 1107, 1110,
                    1113, 1116, 1119, 1122, 1125, 1128, 1131, 1134, 1137, 1140, 1143,
                    1146, 1149, 1152, 1155, 1158, 1161, 1164, 1167, 1170, 1173, 1176,
                    1179, 1182, 1185, 1188, 1191, 1194, 1197, 1200, 1203, 1206, 1209,
                    1212, 1215, 1218, 1221, 1224, 1227, 1230, 1233, 1236, 1239, 1242,
                    1245, 1248, 1251, 1254, 1257, 1260, 1263, 1266, 1269, 1272, 1275,
                    1278, 1281, 1284, 1287, 1290, 1293, 1296, 1299, 1302, 1305, 1308,
                    1311, 1314, 1317, 1320, 1323, 1326, 1329, 1332, 1335, 1338, 1341,
                    1344, 1347, 1350, 1353, 1356, 1359, 1362, 1365, 1368, 1371, 1374,
                    1377, 1380, 1383, 1386, 1389, 1392, 1395, 1398, 1401, 1404, 1407,
                    1410, 1413, 1416, 1419, 1422, 1425, 1428, 1431, 1434, 1437, 1440,
                    1443, 1446, 1449, 1452, 1455, 1458, 1461, 1464, 1467, 1470, 1473,
                    1476, 1479, 1482, 1485, 1488, 1491, 1494, 1497, 1500, 1503, 1506,
                    1509, 1512, 1515, 1518, 1521, 1524, 1527, 1530, 1533, 1536, 1539,
                    1542, 1545, 1548, 1551, 1554, 1557, 1560, 1563, 1566, 1569, 1572,
                    1575, 1578, 1581, 1584, 1587, 1590, 1593, 1596, 1599, 1602, 1605,
                    1608, 1611, 1614, 1617, 1620, 1623, 1626, 1629, 1632, 1635, 1638,
                    1641, 1644, 1647, 1650, 1653, 1656, 1659, 1662, 1665, 1668, 1671,
                    1674, 1677, 1680, 1683, 1686, 1689, 1692, 1695, 1698, 1701, 1704,
                    1707, 1710, 1713, 1716, 1719, 1722, 1725, 1728, 1731, 1734, 1737,
                    1740, 1743, 1746, 1749, 1752, 1755, 1758, 1761, 1764, 1767, 1770,
                    1773, 1776, 1779, 1782, 1785, 1788, 1791, 1794, 1797, 1800, 1803,
                    1806, 1809, 1812, 1815, 1818, 1821, 1824, 1827, 1830, 1833, 1836,
                    1839, 1842, 1845, 1848, 1851, 1854, 1857, 1860, 1863, 1866, 1869,
                    1872, 1875, 1878, 1881, 1884, 1887, 1890, 1893, 1896, 1899, 1902,
                    1905, 1908, 1911, 1914, 1917, 920, 923, 926, 929, 932, 935, 938, 941,
                    944, 947, 950, 953, 956, 959, 962, 965, 968, 971, 974, 977, 980, 983,
                    986, 989, 992, 995, 998, 1001, 1004, 1007, 1010, 1013, 1016, 1019,
                    1022, 1025, 1028, 1031, 1034, 1037, 1040, 1043, 1046, 1049, 1052,
                    1055, 1058, 1061, 1064, 1067, 1070, 1073, 1076, 1079, 1082, 1085,
                    1088, 1091, 1094, 1097, 1100, 1103, 1106, 1109, 1112, 1115, 1118,
                    1121, 1124, 1127, 1130, 1133, 1136, 1139, 1142, 1145, 1148, 1151,
                    1154, 1157
                ), result)
        }

        parseAPLExpression("+/[3] 3 3 4 5 6 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(3, 3, 4, 6), result)
            assertArrayContent(
                arrayOf(
                    60, 65, 70, 75, 80, 85, 210, 215, 220, 225, 230, 235, 360, 365, 370,
                    375, 380, 385, 510, 515, 520, 525, 530, 535, 660, 665, 670, 675, 680,
                    685, 810, 815, 820, 825, 830, 835, 960, 965, 970, 975, 980, 985, 1110,
                    1115, 1120, 1125, 1130, 1135, 1260, 1265, 1270, 1275, 1280, 1285,
                    1410, 1415, 1420, 1425, 1430, 1435, 1560, 1565, 1570, 1575, 1580,
                    1585, 1710, 1715, 1720, 1725, 1730, 1735, 1860, 1865, 1870, 1875,
                    1880, 1885, 2010, 2015, 2020, 2025, 2030, 2035, 2160, 2165, 2170,
                    2175, 2180, 2185, 2310, 2315, 2320, 2325, 2330, 2335, 2460, 2465,
                    2470, 2475, 2480, 2485, 2610, 2615, 2620, 2625, 2630, 2635, 2760,
                    2765, 2770, 2775, 2780, 2785, 2910, 2915, 2920, 2925, 2930, 2935,
                    3060, 3065, 3070, 3075, 3080, 3085, 3210, 3215, 3220, 3225, 3230,
                    3235, 3360, 3365, 3370, 3375, 3380, 3385, 3510, 3515, 3520, 3525,
                    3530, 3535, 3660, 3665, 3670, 3675, 3680, 3685, 3810, 3815, 3820,
                    3825, 3830, 3835, 3960, 3965, 3970, 3975, 3980, 3985, 4110, 4115,
                    4120, 4125, 4130, 4135, 4260, 4265, 4270, 4275, 4280, 4285, 4410,
                    4415, 4420, 4425, 4430, 4435, 4560, 4565, 4570, 4575, 4580, 4585,
                    4710, 4715, 4720, 4725, 4730, 4735, 4860, 4865, 4870, 4875, 4880,
                    4885, 2010, 2015, 2020, 2025, 1030, 1035, 160, 165, 170, 175, 180,
                    185, 310, 315, 320, 325, 330, 335
                ), result)
        }
    }

    private fun reduceTestWithFunctionName(aplFn: String, correctRes: Int) {
        val result = parseAPLExpression("${aplFn}/0⍴4")
        assertTrue(result.dimensions.compareEquals(emptyDimensions()))
        assertSimpleNumber(correctRes.toLong(), result)
    }
}
