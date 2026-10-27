package dsltypes.values

import editor.objects.triggers.counters.ItemTriggerAbsNeg
import editor.objects.triggers.counters.ItemTriggerRoundingType
import editor.objects.triggers.counters.ItemTriggerTermType
import editor.objects.triggers.counters.LabelType
import editor.rawstring.property.getOrThrow
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import provider.TriggProvider
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.random.Random
import kotlin.random.nextUInt

private class GDValuesTests {
    fun <T : GDValue> doCtorTest(value: T, extraTest: (T) -> Unit = {}) {
        fun ItemTriggerRoundingType.getGDValueChanger(): (GDValue) -> GDValue =
            when (this) {
                ItemTriggerRoundingType.NONE -> { value -> value }
                ItemTriggerRoundingType.ROUND -> { value -> round(value) }
                ItemTriggerRoundingType.FLOOR -> { value -> floor(value) }
                ItemTriggerRoundingType.CEIL -> { value -> ceil(value) }
            }

        fun ItemTriggerAbsNeg.getGDValueChanger(): (GDValue) -> GDValue =
            when (this) {
                ItemTriggerAbsNeg.NONE -> { value -> value }
                ItemTriggerAbsNeg.ABS -> { value -> abs(value) }
                ItemTriggerAbsNeg.NEGATIVE -> { value -> negative(value) }
            }

        for (roundingType in ItemTriggerRoundingType.entries) {
            for (absNegType in ItemTriggerAbsNeg.entries) {
                roundingType.getGDValueChanger()(value)
                absNegType.getGDValueChanger()(value)
                Assertions.assertEquals(roundingType, value.roundingType)
                Assertions.assertEquals(absNegType, value.absNegType)

                extraTest(value)

                // We need to reset this value's modifiers because
                // this test would fail otherwise
                //
                // Using these "round", "abs", etc.  functions we
                // cannot reset a modifier, so resetting it
                // manually prevents this test from having
                // unexpected data
                // (you probably did not understand this)
                value.resetModifiers()
            }
        }
    }

    @Test
    @DisplayName("Constructor tests")
    fun ctorTests() {
        val intValue = Random.nextInt()
        doCtorTest(const(intValue)) {
            Assertions.assertEquals(intValue, it.value)
        }

        val floatValue = Random.nextFloat()
        doCtorTest(constFloat(floatValue)) {
            Assertions.assertEquals(floatValue, it.value)
        }

        doCtorTest(item())
        doCtorTest(item(Random.nextUInt()))
        doCtorTest(timer(Random.nextUInt()))
        doCtorTest(points())
        doCtorTest(gameTime())
        doCtorTest(attempts())
    }

    @Test
    @DisplayName("GDValue.applyModifiers() test")
    fun applyModifiersTest() {
        val intValue = -11
        val uintValue = 17u
        val floatValue = 76.1285f
        val doubleValue = -0.7963

        val applyRounding = fun ItemTriggerRoundingType.(num: Double): Double =
            when (this) {
                ItemTriggerRoundingType.NONE -> num
                ItemTriggerRoundingType.ROUND -> round(num)
                ItemTriggerRoundingType.FLOOR -> floor(num)
                ItemTriggerRoundingType.CEIL -> ceil(num)
            }
        val applyAbsNeg = fun ItemTriggerAbsNeg.(num: Double): Double =
            when (this) {
                ItemTriggerAbsNeg.NONE -> num
                ItemTriggerAbsNeg.ABS -> abs(num)
                ItemTriggerAbsNeg.NEGATIVE -> -num
            }

        val value = GDValue(type = ItemTriggerTermType.UNSET)
        for (roundingType in ItemTriggerRoundingType.entries) {
            for (absNegType in ItemTriggerAbsNeg.entries) {
                value.roundingType = roundingType
                value.absNegType = absNegType

                Assertions.assertEquals(
                    absNegType.applyAbsNeg(intValue.toDouble()).toInt(),
                    value.applyModifiers(intValue)
                )

                if (absNegType != ItemTriggerAbsNeg.NEGATIVE) {
                    Assertions.assertEquals(
                        absNegType.applyAbsNeg(uintValue.toDouble()).toUInt(),
                        value.applyModifiers(uintValue)
                    )
                }

                Assertions.assertEquals(
                    absNegType.applyAbsNeg(roundingType.applyRounding(floatValue.toDouble())).toFloat(),
                    value.applyModifiers(floatValue)
                )

                Assertions.assertEquals(
                    absNegType.applyAbsNeg(roundingType.applyRounding(doubleValue)),
                    value.applyModifiers(doubleValue)
                )
            }
        }
    }

    @Test
    @DisplayName("NumberGDValue.with() test")
    fun withNewNumberValueTest() {
        val value = constFloat(7.5f)
        Assertions.assertEquals(7.5f, value.value)

        val value2 = value.with(2.45f)
        Assertions.assertEquals(2.45f, value2.value)
        Assertions.assertEquals(value.absNegType, value2.absNegType)
        Assertions.assertEquals(value.roundingType, value2.roundingType)
        Assertions.assertEquals(value.type, value2.type)
        Assertions.assertNotEquals(value.expectedValue, value2.expectedValue)

        Assertions.assertEquals(7.5f, value.value)
        Assertions.assertNotSame(value, value2)
    }

    @Test
    @DisplayName("Label creation tests")
    fun labelTests() {
        val ctx = TriggProvider()

        ctx.apply {
            val itemValue = item(17u)
            val itemCounter = itemValue.createCounter()
            Assertions.assertEquals(itemValue.itemID, itemCounter.itemID.value)
            Assertions.assertEquals(LabelType.UNSET, itemCounter.labelType.value)
            Assertions.assertFalse(itemCounter.timeCounter.getOrThrow())
            Assertions.assertFalse(itemCounter.secondsOnly.getOrThrow())


            val timerValue = timer(17u)
            val timerCounter = timerValue.createCounter()
            Assertions.assertEquals(timerValue.itemID, timerCounter.itemID.value)
            Assertions.assertEquals(LabelType.UNSET, timerCounter.labelType.value)
            Assertions.assertTrue(timerCounter.timeCounter.getOrThrow())


            val mainTimeValue = gameTime()
            val mainTimeCounter = mainTimeValue.createCounter()
            Assertions.assertTrue(mainTimeCounter.itemID.isDefaultValue())
            Assertions.assertEquals(LabelType.MAIN_TIME, mainTimeCounter.labelType.value)
            Assertions.assertFalse(mainTimeCounter.timeCounter.getOrThrow())
            Assertions.assertFalse(mainTimeCounter.secondsOnly.getOrThrow())


            val attemptsValue = attempts()
            val attemptsCounter = attemptsValue.createCounter()
            Assertions.assertTrue(attemptsCounter.itemID.isDefaultValue())
            Assertions.assertEquals(LabelType.ATTEMPTS, attemptsCounter.labelType.value)
            Assertions.assertFalse(attemptsCounter.timeCounter.getOrThrow())
            Assertions.assertFalse(attemptsCounter.secondsOnly.getOrThrow())


            val pointsValue = points()
            val pointsCounter = pointsValue.createCounter()
            Assertions.assertTrue(pointsCounter.itemID.isDefaultValue())
            Assertions.assertEquals(LabelType.POINTS, pointsCounter.labelType.value)
            Assertions.assertFalse(pointsCounter.timeCounter.getOrThrow())
            Assertions.assertFalse(pointsCounter.secondsOnly.getOrThrow())
        }
    }
}