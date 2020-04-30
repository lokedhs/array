package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TransposeTest : APLTest() {
    @Test
    fun test2DTransposeEmptyLeftArg() {
        val result = parseAPLExpression("⍉ 2 3 ⍴ ⍳6")
        assertDimension(dimensionsOfSize(3, 2), result)
        assertArrayContent(arrayOf(0, 3, 1, 4, 2, 5), result)
    }

    @Test
    fun test2DTranspose() {
        val result = parseAPLExpression("1 0 ⍉ 2 3 ⍴ ⍳6")
        assertDimension(dimensionsOfSize(3, 2), result)
        assertArrayContent(arrayOf(0, 3, 1, 4, 2, 5), result)
    }                //0, 4, 8, 12, 16, 20, 1, 5, 9, 13, 17, 21, 2, 6, 10, 14, 18, 22, 3, 7, 11, 15, 19, 23

    @Test
    fun test3DTransposeEmptyLeftArg() {
        val result = parseAPLExpression("⍉ 3 4 5 ⍴ ⍳60")
        assertDimension(dimensionsOfSize(5, 4, 3), result)
        assertArrayContent(
            arrayOf(
                0, 20, 40, 5, 25, 45, 10, 30, 50, 15, 35, 55, 1, 21, 41, 6, 26, 46,
                11, 31, 51, 16, 36, 56, 2, 22, 42, 7, 27, 47, 12, 32, 52, 17, 37, 57,
                3, 23, 43, 8, 28, 48, 13, 33, 53, 18, 38, 58, 4, 24, 44, 9, 29, 49,
                14, 34, 54, 19, 39, 59
            ),
            result
        )
    }

    @Test
    fun test4DTranspose() {
        val result = parseAPLExpression("2 3 0 1 ⍉ 2 3 4 5 ⍴ ⍳120")
        assertDimension(dimensionsOfSize(4, 5, 2, 3), result)
        assertArrayContent(
            arrayOf(
                0, 20, 40, 60, 80, 100, 1, 21, 41, 61, 81, 101, 2, 22, 42, 62, 82,
                102, 3, 23, 43, 63, 83, 103, 4, 24, 44, 64, 84, 104, 5, 25, 45, 65,
                85, 105, 6, 26, 46, 66, 86, 106, 7, 27, 47, 67, 87, 107, 8, 28, 48,
                68, 88, 108, 9, 29, 49, 69, 89, 109, 10, 30, 50, 70, 90, 110, 11, 31,
                51, 71, 91, 111, 12, 32, 52, 72, 92, 112, 13, 33, 53, 73, 93, 113, 14,
                34, 54, 74, 94, 114, 15, 35, 55, 75, 95, 115, 16, 36, 56, 76, 96, 116,
                17, 37, 57, 77, 97, 117, 18, 38, 58, 78, 98, 118, 19, 39, 59, 79, 99,
                119
            ),
            result
        )
    }

    @Test
    fun renderTransposedArray() {
        val result = parseAPLExpression("2 3 1 0 ⍉ 2 3 4 5 ⍴ ⍳120")
        assertTrue(result.formatted(FormatStyle.PRETTY).length > 100)
    }

    @Test
    fun test5DTranspose() {
        val result = parseAPLExpression("1 4 2 0 3 ⍉ 2 3 4 5 6 ⍴ ⍳720")
        assertDimension(dimensionsOfSize(5, 2, 4, 6, 3), result)
        assertArrayContent(
            arrayOf(
                0, 120, 240, 1, 121, 241, 2, 122, 242, 3, 123, 243, 4, 124, 244, 5,
                125, 245, 30, 150, 270, 31, 151, 271, 32, 152, 272, 33, 153, 273, 34,
                154, 274, 35, 155, 275, 60, 180, 300, 61, 181, 301, 62, 182, 302, 63,
                183, 303, 64, 184, 304, 65, 185, 305, 90, 210, 330, 91, 211, 331, 92,
                212, 332, 93, 213, 333, 94, 214, 334, 95, 215, 335, 360, 480, 600,
                361, 481, 601, 362, 482, 602, 363, 483, 603, 364, 484, 604, 365, 485,
                605, 390, 510, 630, 391, 511, 631, 392, 512, 632, 393, 513, 633, 394,
                514, 634, 395, 515, 635, 420, 540, 660, 421, 541, 661, 422, 542, 662,
                423, 543, 663, 424, 544, 664, 425, 545, 665, 450, 570, 690, 451, 571,
                691, 452, 572, 692, 453, 573, 693, 454, 574, 694, 455, 575, 695, 6,
                126, 246, 7, 127, 247, 8, 128, 248, 9, 129, 249, 10, 130, 250, 11,
                131, 251, 36, 156, 276, 37, 157, 277, 38, 158, 278, 39, 159, 279, 40,
                160, 280, 41, 161, 281, 66, 186, 306, 67, 187, 307, 68, 188, 308, 69,
                189, 309, 70, 190, 310, 71, 191, 311, 96, 216, 336, 97, 217, 337, 98,
                218, 338, 99, 219, 339, 100, 220, 340, 101, 221, 341, 366, 486, 606,
                367, 487, 607, 368, 488, 608, 369, 489, 609, 370, 490, 610, 371, 491,
                611, 396, 516, 636, 397, 517, 637, 398, 518, 638, 399, 519, 639, 400,
                520, 640, 401, 521, 641, 426, 546, 666, 427, 547, 667, 428, 548, 668,
                429, 549, 669, 430, 550, 670, 431, 551, 671, 456, 576, 696, 457, 577,
                697, 458, 578, 698, 459, 579, 699, 460, 580, 700, 461, 581, 701, 12,
                132, 252, 13, 133, 253, 14, 134, 254, 15, 135, 255, 16, 136, 256, 17,
                137, 257, 42, 162, 282, 43, 163, 283, 44, 164, 284, 45, 165, 285, 46,
                166, 286, 47, 167, 287, 72, 192, 312, 73, 193, 313, 74, 194, 314, 75,
                195, 315, 76, 196, 316, 77, 197, 317, 102, 222, 342, 103, 223, 343,
                104, 224, 344, 105, 225, 345, 106, 226, 346, 107, 227, 347, 372, 492,
                612, 373, 493, 613, 374, 494, 614, 375, 495, 615, 376, 496, 616, 377,
                497, 617, 402, 522, 642, 403, 523, 643, 404, 524, 644, 405, 525, 645,
                406, 526, 646, 407, 527, 647, 432, 552, 672, 433, 553, 673, 434, 554,
                674, 435, 555, 675, 436, 556, 676, 437, 557, 677, 462, 582, 702, 463,
                583, 703, 464, 584, 704, 465, 585, 705, 466, 586, 706, 467, 587, 707,
                18, 138, 258, 19, 139, 259, 20, 140, 260, 21, 141, 261, 22, 142, 262,
                23, 143, 263, 48, 168, 288, 49, 169, 289, 50, 170, 290, 51, 171, 291,
                52, 172, 292, 53, 173, 293, 78, 198, 318, 79, 199, 319, 80, 200, 320,
                81, 201, 321, 82, 202, 322, 83, 203, 323, 108, 228, 348, 109, 229,
                349, 110, 230, 350, 111, 231, 351, 112, 232, 352, 113, 233, 353, 378,
                498, 618, 379, 499, 619, 380, 500, 620, 381, 501, 621, 382, 502, 622,
                383, 503, 623, 408, 528, 648, 409, 529, 649, 410, 530, 650, 411, 531,
                651, 412, 532, 652, 413, 533, 653, 438, 558, 678, 439, 559, 679, 440,
                560, 680, 441, 561, 681, 442, 562, 682, 443, 563, 683, 468, 588, 708,
                469, 589, 709, 470, 590, 710, 471, 591, 711, 472, 592, 712, 473, 593,
                713, 24, 144, 264, 25, 145, 265, 26, 146, 266, 27, 147, 267, 28, 148,
                268, 29, 149, 269, 54, 174, 294, 55, 175, 295, 56, 176, 296, 57, 177,
                297, 58, 178, 298, 59, 179, 299, 84, 204, 324, 85, 205, 325, 86, 206,
                326, 87, 207, 327, 88, 208, 328, 89, 209, 329, 114, 234, 354, 115,
                235, 355, 116, 236, 356, 117, 237, 357, 118, 238, 358, 119, 239, 359,
                384, 504, 624, 385, 505, 625, 386, 506, 626, 387, 507, 627, 388, 508,
                628, 389, 509, 629, 414, 534, 654, 415, 535, 655, 416, 536, 656, 417,
                537, 657, 418, 538, 658, 419, 539, 659, 444, 564, 684, 445, 565, 685,
                446, 566, 686, 447, 567, 687, 448, 568, 688, 449, 569, 689, 474, 594,
                714, 475, 595, 715, 476, 596, 716, 477, 597, 717, 478, 598, 718, 479,
                599, 719
            ),
            result
        )
    }

    @Test
    fun transposeSingleTestEmptyLeftArg() {
        val result = parseAPLExpression("⍉3")
        assertDimension(emptyDimensions(), result)
        assertEquals(3, result.ensureNumber().asLong())
    }

    @Test
    fun transposeSingleTest() {
        val result = parseAPLExpression("(0⍴7) ⍉3")
        assertDimension(emptyDimensions(), result)
        assertEquals(3, result.ensureNumber().asLong())
    }

    @Test
    fun errorWithIncorrectAxisCount() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 ⍉ 3 4 5 ⍴ ⍳60")
        }
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 2 3 ⍉ 3 4 5 ⍴ ⍳60")
        }
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 3 ⍉ 3 4 5 ⍴ ⍳60")
        }
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 3 ⍉ 3 4 5 ⍴ ⍳60")
        }
    }

    @Test
    fun inverseHorizontalTest() {
        parseAPLExpression("⌽4 5 4 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(4, 5, 4), result)
            assertArrayContent(arrayOf(
                3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12, 19, 18, 17, 16, 23, 22,
                21, 20, 27, 26, 25, 24, 31, 30, 29, 28, 35, 34, 33, 32, 39, 38, 37, 36, 43,
                42, 41, 40, 47, 46, 45, 44, 51, 50, 49, 48, 55, 54, 53, 52, 59, 58, 57, 56, 63,
                62, 61, 60, 67, 66, 65, 64, 71, 70, 69, 68, 75, 74, 73, 72, 79, 78, 77, 76), result)
        }
        parseAPLExpression("⌽1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(4, 3, 2, 1), result)
        }
        parseAPLExpression("⌽1").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun inverseVerticalTest() {
        parseAPLExpression("⊖4 5 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(4, 5), result)
            assertArrayContent(arrayOf(15, 16, 17, 18, 19, 10, 11, 12, 13, 14, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4), result)
        }
        parseAPLExpression("⊖1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(4, 3, 2, 1), result)
        }
        parseAPLExpression("⊖1").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun rotateHorizontalTest() {
        parseAPLExpression("1⌽1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(2, 3, 4, 1), result)
        }
    }

    @Test
    fun rotateVerticalTest() {
        parseAPLExpression("1⊖4 5 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(4, 5), result)
            assertArrayContent(arrayOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 0, 1, 2, 3, 4), result)
        }
    }
}
