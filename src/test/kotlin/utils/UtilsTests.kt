package utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private class UtilsTests {
    @Test
    fun groupClampingTest() {
        val range1 = 5u..743u
        Assertions.assertEquals(range1, range1.groupClamping(includeZero = false))
        Assertions.assertEquals(range1, range1.groupClamping(includeZero = true))

        val range2 = 0u..134u
        Assertions.assertEquals(1u..134u, range2.groupClamping(includeZero = false))
        Assertions.assertEquals(range2, range2.groupClamping(includeZero = true))

        val range3 = 47u..10793u
        Assertions.assertEquals(47u..9999u, range3.groupClamping(includeZero = false))
        Assertions.assertEquals(47u..9999u, range3.groupClamping(includeZero = true))

        val range4 = 0u..10793u
        Assertions.assertEquals(1u..9999u, range4.groupClamping(includeZero = false))
        Assertions.assertEquals(0u..9999u, range4.groupClamping(includeZero = true))
    }

    @Test
    fun uintRangeOverlapTest() {
        val range1 = 7u..12u
        val range2 = 9u..16u
        val range3 = 16u..24u

        Assertions.assertTrue(range1.overlaps(range1))
        Assertions.assertTrue(range1.overlaps(range2))
        Assertions.assertFalse(range1.overlaps(range3))

        Assertions.assertTrue(range2.overlaps(range2))
        Assertions.assertTrue(range2.overlaps(range1))
        Assertions.assertTrue(range2.overlaps(range3))

        Assertions.assertTrue(range3.overlaps(range3))
        Assertions.assertFalse(range3.overlaps(range1))
        Assertions.assertTrue(range3.overlaps(range2))
    }
}