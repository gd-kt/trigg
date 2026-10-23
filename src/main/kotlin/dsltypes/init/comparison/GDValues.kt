package dsltypes.init.comparison

import editor.objects.SimpleObject
import editor.objects.triggers.counters.ItemCompareComparisonType
import editor.objects.triggers.counters.ItemCompareTrigger
import editor.objects.triggers.counters.ItemTriggerAbsNeg
import editor.objects.triggers.counters.ItemTriggerRoundingType
import editor.objects.triggers.counters.ItemTriggerTermType
import provider.GroupHandler
import provider.ObjectManager
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

typealias ComparisonCreator = ObjectManager<SimpleObject>.() -> Comparison

/**
 * A value understandable by geometry dash.
 * It can store one of:
 * - A number (int/float)
 * - A timer
 * - Points
 * - Time
 * - Attempts
 *
 * Custom "modifiers" can also be set, for example:
 * ```kotlin
 * round(const(5))
 * ```
 *
 * creates a constant [GDValue] with a value of `5` which will be rounded.
 * Here it doesn't do anything since `round(5) == 5` but this is an example.
 *
 * ## Constructors
 *
 * Since there are multiple types of [GDValues][GDValue], there are multiple constructors:
 * - [`const(Number)`][const] and [`const(Uint)`][const]: Creates a constant from any number.
 * The value **gets cast into an int**.
 * - [`constFloat(Number)`][constFloat]: Creates a float constant from any number.
 * The value **gets cast into a float**.
 * - [`item(UInt?)`][item]: Creates an [Item Value][ItemGDValue] with an optional itemID. Learn more in [item]'s KDoc.
 * - [`timer(UInt)`][timer]: Creates a [Timer Value][TimerGDValue] with a timerID.
 * - [`points()`][points]: Creates a [Points Value][PointsGDValue].
 * It stores the current points of a run. This is only used in **Platformer Mode**.
 * - [`gameTime()`][gameTime]: Creates a [Game Timer Value][GameTimeGDValue].
 * It stores the current time the level was played for in a 2 decimal precision timer (ex: `5.16`).
 * This only works in **Platformer Mode**.
 * Its behavior is special. If the player hasn't activated any checkpoint
 * then on death the timer will reset.
 * But when a checkpoint got activated, unless a full restart is triggered, the
 * timer won't stop.
 * - [`attempts()`][attempts]: Creates an [Attempt Value][AttemptGDValue].
 * It stores the current attempt count for this session.
 *
 * ## Modifiers
 *
 * Modifiers are what allows to modify a value at runtime.
 *
 * 2 modifiers **of the same type** cannot be applied
 * at the same time.
 * For example `ceil(round(const(18)))` will not work and
 * will translate to `ceil(const(18))` instead.
 *
 * ---
 *
 * Here are all the modifiers listed by types:
 *
 * **Rounding:**
 * - [`round()`][round]
 * - [`floor()`][floor]
 * - [`ceil()`][ceil]
 *
 * **Abs/Neg:**
 * - [`abs()`][abs]
 * - [`negative()`][negative]
 *
 * @property roundingType the type of the rounding applied to the value at runtime
 * @property absNegType the "abs/neg" type applied to the value at runtime
 * @property type the type of the value, it's what is stored
 * @property allowInlining if when creating a compare trigger
 * the [comparison][Comparison] can be inlined.
 *
 * For example if we're doing the comparison `const(8) greaterThanEqual const(3)`, `8` is
 * clearly greater than `3`, so the "then" branch can be inlined
 * and the "else" branch discarded.
 */
open class GDValue(
    var roundingType: ItemTriggerRoundingType = ItemTriggerRoundingType.NONE,
    var absNegType: ItemTriggerAbsNeg = ItemTriggerAbsNeg.NONE,
    val type: ItemTriggerTermType,
    var allowInlining: Boolean = true
) {
    private fun createCompareTrigger(other: GDValue, compType: ItemCompareComparisonType): ComparisonCreator = { ctx ->
        // When this is the case we can precompute
        // the outcome since it's a simple if check
        if (this@GDValue.allowInlining && this is NumberGDValue<*> && other is NumberGDValue<*>) {
            val a = this.value.toDouble()
            val b = other.value.toDouble()

            val res = when (compType) {
                ItemCompareComparisonType.EQUAL -> Comparison.InlinedBranch.fromEvaluation(a == b)
                ItemCompareComparisonType.NOT_EQUAL -> Comparison.InlinedBranch.fromEvaluation(a != b)
                ItemCompareComparisonType.GREATER_THAN -> Comparison.InlinedBranch.fromEvaluation(a > b)
                ItemCompareComparisonType.GREATER_THAN_EQUAL -> Comparison.InlinedBranch.fromEvaluation(a >= b)
                ItemCompareComparisonType.LESS_THAN -> Comparison.InlinedBranch.fromEvaluation(a < b)
                ItemCompareComparisonType.LESS_THAN_EQUAL -> Comparison.InlinedBranch.fromEvaluation(a <= b)
            }

            Comparison(inlinedBranch = res)
        }

        val itemCompTrigger = ItemCompareTrigger(ctx.getNextPos()).apply {
            comparisonType.value = compType


            firstTermType.value = this@GDValue.type
            if (this@GDValue is WithIdGDValue)
                firstItemID.value = this@GDValue.getItemID(ctx)

            secondTermType.value = other.type
            if (other is WithIdGDValue)
                secondItemID.value = other.getItemID(ctx)

            if (other is NumberGDValue<*>)
                secondTermFactor.value = other.value.toFloat()
        }

        Comparison(itemCompTrigger)
    }

    infix fun equalsTo(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.EQUAL)

    infix fun notEqualTo(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.NOT_EQUAL)

    infix fun greaterThan(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.GREATER_THAN)

    infix fun greaterThanEqual(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.GREATER_THAN_EQUAL)

    infix fun lessThan(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.LESS_THAN)

    infix fun lessThanEqual(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.LESS_THAN_EQUAL)
}

abstract class NumberGDValue<N : Number>(val value: N, type: ItemTriggerTermType = ItemTriggerTermType.ITEM) : GDValue(type = type) {
    protected abstract fun numberToN(numInput: Number): N

    /**
     * Creates a copy of [this][NumberGDValue] but with a new value
     * @param newValue the value to set in the copy
     * @return the copy with the newly set value
     */
    abstract fun with(newValue: N): NumberGDValue<N>

    /**
     * The expected value that will be evaluated by Geometry Dash
     */
    val expectedValue: N
        get() {
            var doubleValue = this.value.toDouble()
            when (this.roundingType) {
                ItemTriggerRoundingType.ROUND -> doubleValue = round(doubleValue)
                ItemTriggerRoundingType.FLOOR -> doubleValue = floor(doubleValue)
                ItemTriggerRoundingType.CEIL -> doubleValue = round(doubleValue)
                else -> {}
            }

            when (this.absNegType) {
                ItemTriggerAbsNeg.ABS -> doubleValue = abs(doubleValue)
                ItemTriggerAbsNeg.NEGATIVE -> doubleValue = -doubleValue
                else -> {}
            }

            return this.numberToN(doubleValue)
        }
}

abstract class WithIdGDValue(val itemID: UInt? = null, type: ItemTriggerTermType) : GDValue(type = type) {
    private var acquiredItemID: UInt? = null;

    /**
     * Gets the itemID of this [GDValue][WithIdGDValue].
     *
     * If its [itemID] is equal to `null` then
     * a new one will be acquired **once**.
     * The acquired label gets cached.
     * @param ctx the context used to acquire a new label ID
     *            if needed
     * @return the id of this [GDValue][WithIdGDValue] label ID
     */
    fun getItemID(ctx: GroupHandler): UInt {
        if (this.acquiredItemID == null)
            this.acquiredItemID = this.itemID ?: ctx.acquireLabel()

        return this.acquiredItemID!!
    }
}

class IntGDValue(value: Int): NumberGDValue<Int>(value) {
    override fun numberToN(numInput: Number): Int = numInput.toInt()

    override fun with(newValue: Int): IntGDValue =
        IntGDValue(newValue).let {
            it.allowInlining = this.allowInlining
            it.absNegType = this.absNegType
            it.roundingType = this.roundingType

            return it
        }
}

class FloatGDValue(value: Float): NumberGDValue<Float>(value) {
    init {
        require(this.value.isFinite()) { "Value for a FloatGDValue must be finite. It is ${this.value} instead" }
    }

    override fun numberToN(numInput: Number): Float = numInput.toFloat()

    override fun with(newValue: Float): FloatGDValue =
        FloatGDValue(newValue).let {
            it.allowInlining = this.allowInlining
            it.absNegType = this.absNegType
            it.roundingType = this.roundingType

            return it
        }
}

class ItemGDValue(itemID: UInt? = null) : WithIdGDValue(itemID, type = ItemTriggerTermType.ITEM)
class TimerGDValue(timerID: UInt) : WithIdGDValue(timerID, type = ItemTriggerTermType.TIMER)
class PointsGDValue : GDValue(type = ItemTriggerTermType.POINTS)
class GameTimeGDValue : GDValue(type = ItemTriggerTermType.TIME)
class AttemptGDValue : GDValue(type = ItemTriggerTermType.ATTEMPTS)

/**
 * Creates an [ItemGDValue] with an optional itemID
 * @param itemID the optional itemID to set for the [ItemGDValue].
 *               If `null`, a new itemID will get [acquired][GroupHandler.acquireLabel]
 */
fun item(itemID: UInt? = null) = ItemGDValue(itemID)

/**
 * Creates an [TimerGDValue] with a timerID
 * @param timerID the **non-optional** timerID to set for the [TimerGDValue]
 */
fun timer(timerID: UInt) = TimerGDValue(timerID)

/**
 * Creates a [Points Value][PointsGDValue].
 * It stores the current points of a run. This is only used in **Platformer Mode**.
 */
fun points() = PointsGDValue()

/**
 * Creates a [Game Timer Value][GameTimeGDValue].
 *
 * It stores the current time the level was played for in a 2 decimal precision timer (ex: `5.16`).
 * This only works in **Platformer Mode**.
 *
 * Its behavior is special. If the player hasn't activated any checkpoint
 * then on death the timer will reset.
 * But when a checkpoint got activated, unless a full restart is triggered, the
 * timer won't stop.
 */
fun gameTime() = GameTimeGDValue()

/**
 * Creates an [Attempt Value][AttemptGDValue].
 * It stores the current attempt count for this session.
 */
fun attempts() = AttemptGDValue()

/**
 * Creates a [IntGDValue] from a constant number.
 * @param value the value to store. It always gets converted to an [Int]
 * @return the created [IntGDValue] from the given [value]
 */
fun const(value: Number) =
    IntGDValue(value.toInt())

/**
 * Creates a [IntGDValue] from a constant number.
 * @param value the value to store. It always gets **converted to an [Int]**
 * @return the created [IntGDValue] from the given [value]
 */
fun const(value: UInt) =
    IntGDValue(value.toInt())

/**
 * Creates a [FloatGDValue] from a constant **float** number.
 * This value if stored in a label will always get floored.
 * GD doesn't store floats. This is only usable for comparisons
 * @param value the value to store. It always **gets converted to an [Float]**
 * @return the created [FloatGDValue] from the given [value]
 */
fun constFloat(value: Number) =
    FloatGDValue(value.toFloat())


fun negative(gdValue: GDValue): GDValue =
    gdValue.apply {
        absNegType = ItemTriggerAbsNeg.NEGATIVE
    }

fun abs(gdValue: GDValue): GDValue =
    gdValue.apply {
        absNegType = ItemTriggerAbsNeg.ABS
    }


fun round(gdValue: GDValue): GDValue =
    gdValue.apply {
        roundingType = ItemTriggerRoundingType.ROUND
    }

fun floor(gdValue: GDValue): GDValue =
    gdValue.apply {
        roundingType = ItemTriggerRoundingType.FLOOR
    }

fun ceil(gdValue: GDValue): GDValue =
    gdValue.apply {
        roundingType = ItemTriggerRoundingType.CEIL
    }
