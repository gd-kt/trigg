package dsltypes.values

import dsltypes.comparison.Comparison
import editor.objects.SimpleObject
import editor.objects.triggers.counters.*
import provider.GroupHandler
import provider.ObjectManager
import kotlin.math.abs
import kotlin.math.ceil
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
 * A [GDValue] can also become persistent, however only if it
 * can contain an itemID or a timerID.
 * In a trigg context you can do:
 * ```kotlin
 * myValue = persistent { const(/* ... */) }
 * ```
 *
 * ## Constructors
 *
 * Since there are multiple types of [GDValues][GDValue], there are multiple constructors:
 * - [const(Number)][const] and [`const(Uint)`][const]: Creates a constant from any number.
 * The value **gets cast into an int**.
 * - [constFloat(Number)][constFloat]: Creates a float constant from any number.
 * The value **gets cast into a float**.
 * - [item(UInt?)][item]: Creates an [Item Value][ItemGDValue] with an optional itemID. Learn more in [item]'s KDoc.
 * - [timer(UInt)][timer]: Creates a [Timer Value][TimerGDValue] with a timerID.
 * - [points()][points]: Creates a [Points Value][PointsGDValue].
 * It stores the current points of a run. This is only used in **Platformer Mode**.
 * - [gameTime()][gameTime]: Creates a [Game Timer Value][GameTimeGDValue].
 * It stores the current time the level was played for in a 2 decimal precision timer (ex: `5.16`).
 * This only works in **Platformer Mode**.
 * Its behavior is special. If the player hasn't activated any checkpoint
 * then on death the timer will reset.
 * But when a checkpoint got activated, unless a full restart is triggered, the
 * timer won't stop.
 * - [attempts()][attempts]: Creates an [Attempt Value][AttemptGDValue].
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
 * - [round()][round]
 * - [floor()][floor]
 * - [ceil()][ceil]
 *
 * **Abs/Neg:**
 * - [abs()][abs]
 * - [negative()][negative]
 *
 * @property roundingType the type of the rounding applied to the value at runtime
 * @property absNegType the "abs/neg" type applied to the value at runtime
 * @property type the type of the value, it's what is stored
 */
open class GDValue(
    var roundingType: ItemTriggerRoundingType = ItemTriggerRoundingType.NONE,
    var absNegType: ItemTriggerAbsNeg = ItemTriggerAbsNeg.NONE,
    val type: ItemTriggerTermType
) {
    private fun createCompareTrigger(other: GDValue, compType: ItemCompareComparisonType): ComparisonCreator =
        ComparisonCreator@{ ctx ->
            // When this is the case we can precompute
            // the outcome since it's a simple if check
            if (this is NumberGDValue<*> && other is NumberGDValue<*>) {
                val a = this.expectedValue.toDouble()
                val b = other.expectedValue.toDouble()

                val inlinedBranch = Comparison.InlinedBranch.fromEvaluation(compType.compare(a, b))
                return@ComparisonCreator Comparison(inlinedBranch = inlinedBranch, operator = compType)
            }

            val itemCompTrigger = ItemCompareTrigger(ctx.getNextPos()).apply {
                comparisonType.value = compType


                firstTermType.value = this@GDValue.type
                firstTermRoundingType.value = this@GDValue.roundingType
                firstTermAbsNeg.value = this@GDValue.absNegType
                if (this@GDValue is WithIdGDValue)
                    firstItemID.value = this@GDValue.getItemID(ctx)

                secondTermRoundingType.value = other.roundingType
                secondTermAbsNeg.value = other.absNegType
                secondTermType.value = other.type
                if (other is WithIdGDValue)
                    secondItemID.value = other.getItemID(ctx)

                if (other is NumberGDValue<*>)
                    secondTermFactor.value = other.value.toFloat()
            }

            Comparison(itemCompTrigger, operator = compType)
        }

    /**
     * Operator `==`
     * @see notEqualTo
     * @see greaterThan
     * @see greaterThanEqual
     * @see lessThan
     * @see lessThanEqual
     */
    infix fun equalsTo(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.EQUAL)

    /**
     * Operator `!=`
     * @see equalsTo
     * @see greaterThan
     * @see greaterThanEqual
     * @see lessThan
     * @see lessThanEqual
     */
    infix fun notEqualTo(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.NOT_EQUAL)

    /**
     * Operator `>`
     * @see equalsTo
     * @see notEqualTo
     * @see greaterThanEqual
     * @see lessThan
     * @see lessThanEqual
     */
    infix fun greaterThan(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.GREATER_THAN)

    /**
     * Operator `>=`
     * @see equalsTo
     * @see notEqualTo
     * @see greaterThan
     * @see lessThan
     * @see lessThanEqual
     */
    infix fun greaterThanEqual(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.GREATER_THAN_EQUAL)

    /**
     * Operator `<`
     * @see equalsTo
     * @see notEqualTo
     * @see greaterThan
     * @see greaterThanEqual
     * @see lessThanEqual
     */
    infix fun lessThan(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.LESS_THAN)

    /**
     * Operator `<=`
     * @see equalsTo
     * @see notEqualTo
     * @see greaterThan
     * @see greaterThanEqual
     * @see lessThan
     */
    infix fun lessThanEqual(other: GDValue): ComparisonCreator =
        this.createCompareTrigger(other, ItemCompareComparisonType.LESS_THAN_EQUAL)
}

abstract class NumberGDValue<N : Number>(val value: N, type: ItemTriggerTermType = ItemTriggerTermType.UNSET) : GDValue(type = type) {
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
            val doubleValue = this.value.toDouble()
            return this.numberToN(this.applyModifiers(doubleValue))
        }
}

abstract class WithIdGDValue(val itemID: UInt? = null, type: ItemTriggerTermType) : GDValue(type = type) {
    private var acquiredItemID: UInt? = null

    /**
     * The last known value manually set **known** by trigg.
     * This doesn't mean that this reflects the true value in **ANY WAY**
     */
    var lastKnownValue: Int? = null
        private set

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

    /**
     * Sets the value of this [GDValue][WithIdGDValue] when this executes.
     * The modifiers applied to this [GDValue][WithIdGDValue] will
     * be applied to the given [value]
     * @param value The value is converted into a [Float]. But no matter
     * what geometry dash will turn the result into an [Int]
     */
    fun setValue(ctx: ObjectManager<in SimpleObject>, value: Number): ItemEditTrigger {
        this.lastKnownValue = value.toInt()
        return ctx.add {
            ItemEditTrigger(ctx.getNextPos()).apply {
                resultItemID.value = this@WithIdGDValue.getItemID(ctx)
                secondTermFactor.value = value.toFloat()

                secondTermAbsNeg.value = this@WithIdGDValue.absNegType
                secondTermRoundingType.value = this@WithIdGDValue.roundingType
            }
        }
    }

    /**
     * Sets the value of this [GDValue][WithIdGDValue] when this executes.
     */
    fun setValue(ctx: ObjectManager<in SimpleObject>, value: UInt) =
        this.setValue(ctx, value.toInt())
}

context(ctx: ObjectManager<in SimpleObject>)
fun WithIdGDValue.setValue(value: Number) =
    this.setValue(ctx, value)

context(ctx: ObjectManager<in SimpleObject>)
fun WithIdGDValue.setValue(value: UInt) =
    this.setValue(ctx, value)

class IntGDValue(value: Int): NumberGDValue<Int>(value) {
    override fun numberToN(numInput: Number): Int = numInput.toInt()

    override fun with(newValue: Int): IntGDValue =
        IntGDValue(newValue).apply {
            absNegType = this.absNegType
            roundingType = this.roundingType
        }
}

class FloatGDValue(value: Float): NumberGDValue<Float>(value) {
    init {
        require(this.value.isFinite()) { "Value for a FloatGDValue must be finite. However it is ${this.value}" }
    }

    override fun numberToN(numInput: Number): Float = numInput.toFloat()

    override fun with(newValue: Float): FloatGDValue =
        FloatGDValue(newValue).apply {
            absNegType = this.absNegType
            roundingType = this.roundingType
        }
}

/**
 * Applies the modifiers of this [value][GDValue] to the given [value].
 * They are applied like this: `absNeg(roundingType(value))`, this is the order
 * in which Geometry Dash applies it.
 *
 * To learn more about modifiers, read [GDValue]'s KDoc
 */
fun GDValue.applyModifiers(value: Int): Int {
    return when (this.absNegType) {
        ItemTriggerAbsNeg.ABS -> abs(value)
        ItemTriggerAbsNeg.NEGATIVE -> -value
        else -> value
    }
}

/**
 * Applies the modifiers of this [value][GDValue] to the given [value].
 * They are applied like this: `absNeg(roundingType(value))`, this is the order
 * in which Geometry Dash applies it.
 *
 * To learn more about modifiers, read [GDValue]'s KDoc
 */
fun GDValue.applyModifiers(value: Double): Double {
    var res = value
    when (this.roundingType) {
        ItemTriggerRoundingType.ROUND -> res = round(res)
        ItemTriggerRoundingType.FLOOR -> res = floor(res)
        ItemTriggerRoundingType.CEIL -> res = ceil(res)
        else -> {}
    }

    when (this.absNegType) {
        ItemTriggerAbsNeg.ABS -> res = abs(res)
        ItemTriggerAbsNeg.NEGATIVE -> res = -res
        else -> {}
    }

    return res
}

/**
 * Applies the modifiers of this [value][GDValue] to the given [value].
 * They are applied like this: `absNeg(roundingType(value))`, this is the order
 * in which Geometry Dash applies it.
 *
 * To learn more about modifiers, read [GDValue]'s KDoc
 */
fun GDValue.applyModifiers(value: Float): Float =
    this.applyModifiers(value.toDouble()).toFloat()

/**
 * Applies the modifiers of this [value][GDValue] to the given [value].
 * To learn more about modifiers, read [GDValue]'s KDoc
 */
fun GDValue.applyModifiers(value: UInt): UInt =
    this.applyModifiers(value.toInt()).toUInt()

/**
 * Resets the modifiers of this [GDValue]
 * @see roundingType
 * @see absNegType
 */
fun GDValue.resetModifiers() {
    this.roundingType = ItemTriggerRoundingType.NONE
    this.absNegType = ItemTriggerAbsNeg.NONE
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
 * Creates an [ItemGDValue] with an optional itemID and a value
 * @param itemID the optional itemID to set for the [ItemGDValue].
 *               If `null`, a new itemID will get [acquired][GroupHandler.acquireLabel]
 * @param value the value to set
 * @see ItemGDValue.setValue
 */
fun ObjectManager<SimpleObject>.item(itemID: UInt? = null, value: Number): ItemGDValue {
    val gdValue = ItemGDValue(itemID)
    gdValue.setValue(this, value)

    return gdValue
}

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
 *
 * To create a constant float value, use [constFloat].
 * @param value the value to store. It always gets **converted to an [Int]**
 * @return the created [IntGDValue] from the given [value]
 * @see constFloat
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
 * GD doesn't store floats. This is only usable for comparisons.
 *
 * To create a constant integer value, use [const].
 * @param value the value to store. It always **gets converted to an [Float]**
 * @return the created [FloatGDValue] from the given [value]
 * @see const
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
