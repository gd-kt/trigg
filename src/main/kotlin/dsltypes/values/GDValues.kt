package dsltypes.values

import dsltypes.comparison.Comparison
import editor.objects.SimpleObject
import editor.objects.data.GridPos
import editor.objects.data.Position
import editor.objects.triggers.counters.*
import provider.GroupHandler
import provider.ObjectManager
import utils.toInt
import java.util.Objects
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

typealias ComparisonCreator = ObjectManager<SimpleObject>.() -> Comparison

interface ValueWithCounter {
    /**
     * Creates a [CounterLabel][CounterLabelTrigger] of this [GDValue]
     * @param ctx the context in which to place the object
     * @param pos the pos where to create the object
     * @return the created [CounterLabel][CounterLabelTrigger]
     */
    context(ctx: ObjectManager<SimpleObject>)
    fun createCounter(pos: Position = ctx.getNextPos() + GridPos(5f, 0f)): CounterLabelTrigger
}

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
 * - [const(Number)][const] and [const(Uint)][const]: Creates a constant from any number.
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
 * - [negate()][negate]
 *
 * @property roundingType the type of the rounding applied to the value at runtime
 * @property absNegType the "abs/neg" type applied to the value at runtime
 * @property type the type of the value, it's what is stored
 * @property isValueGlobal if this [GDValue]'s value is stored globally.
 * For example an [AttemptGDValue] stores 1 global value: the attempt count.
 * But an [ItemGDValue] stores a value per ID, not 1 global value.
 */
open class GDValue(
    var roundingType: ItemTriggerRoundingType = ItemTriggerRoundingType.NONE,
    var absNegType: ItemTriggerAbsNeg = ItemTriggerAbsNeg.NONE,
    val isValueGlobal: Boolean = false,
    val type: ItemTriggerTermType
) {
    private fun createCompareTrigger(other: GDValue, compType: ItemCompareComparisonType): ComparisonCreator =
        ComparisonCreator@{ ctx ->
            // This part is used to fix when the user
            // compares a constant to something else
            // but that the constant is given as the
            // first term. In vanilla gd this cannot be done.
            // So we swap the values if needed
            val shouldSwapValues = this is NumberGDValue<*> && other !is NumberGDValue<*> // TODO: test
            val actualCompType: ItemCompareComparisonType =
                if (shouldSwapValues && compType != ItemCompareComparisonType.EQUAL && compType != ItemCompareComparisonType.NOT_EQUAL)
                    compType.opposite()
                else
                    compType
            
            val actualThis = if (shouldSwapValues)
                other
            else
                this

            val actualOther = if (shouldSwapValues)
                this
            else
                other


            // When this is the case we can precompute
            // the outcome since it's a simple if check
            if (this.type == other.type && this.isValueGlobal && other.isValueGlobal) {
                val dummyValue = 0.0
                val a = if (actualThis is NumberGDValue<*>)
                    actualThis.expectedValue.toDouble()
                else
                    dummyValue

                val b = if (actualOther is NumberGDValue<*>)
                    actualOther.expectedValue.toDouble()
                else
                    dummyValue

                val inlinedBranch = Comparison.InlinedBranch.fromEvaluation(actualCompType.compare(a, b))
                return@ComparisonCreator Comparison(inlinedBranch = inlinedBranch, operator = actualCompType)
            }

            val itemCompTrigger = ItemCompareTrigger(ctx.getNextPos()).apply {
                comparisonType.value = actualCompType


                firstTermType.value = actualThis.type
                firstTermRoundingType.value = actualThis.roundingType
                firstTermAbsNeg.value = actualThis.absNegType
                if (actualThis is WithIdGDValue)
                    firstItemID.value = actualThis.getItemID(ctx)

                secondTermRoundingType.value = actualOther.roundingType
                secondTermAbsNeg.value = actualOther.absNegType
                secondTermType.value = actualOther.type
                if (actualOther is WithIdGDValue) {
                    secondItemID.value = actualOther.getItemID(ctx)
                } else if (actualOther is NumberGDValue<*>) {
                    secondTermFactor.value = actualOther.value.toFloat()
                }
            }

            Comparison(itemCompTrigger, operator = actualCompType)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GDValue) return false

        if (this.roundingType != other.roundingType) return false
        if (this.absNegType != other.absNegType) return false
        if (this.type != other.type) return false

        return true
    }

    override fun hashCode(): Int =
        Objects.hash(this.roundingType, this.absNegType, this.type)
}

abstract class NumberGDValue<N : Number>(val value: N, type: ItemTriggerTermType = ItemTriggerTermType.UNSET) : GDValue(type = type, isValueGlobal = true) {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NumberGDValue<*>) return false
        if (!super.equals(other)) return false

        if (this.value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + this.value.hashCode()
        return result
    }
}

abstract class WithIdGDValue(val itemID: UInt? = null, type: ItemTriggerTermType) : GDValue(type = type), ValueWithCounter {
    private var acquiredItemID: UInt? = null

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WithIdGDValue) return false
        if (!super.equals(other)) return false

        if (this.itemID != other.itemID || this.acquiredItemID != other.acquiredItemID) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + this.itemID.hashCode()
        return result
    }
}

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

fun ItemCompareComparisonType.getComparison(value: GDValue, other: GDValue): ComparisonCreator =
    when (this) {
        ItemCompareComparisonType.EQUAL -> value.equalsTo(other)
        ItemCompareComparisonType.GREATER_THAN -> value.greaterThan(other)
        ItemCompareComparisonType.GREATER_THAN_EQUAL -> value.greaterThanEqual(other)
        ItemCompareComparisonType.LESS_THAN -> value.lessThan(other)
        ItemCompareComparisonType.LESS_THAN_EQUAL -> value.lessThanEqual(other)
        ItemCompareComparisonType.NOT_EQUAL -> value.notEqualTo(other)
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

class ItemGDValue(itemID: UInt? = null) : WithIdGDValue(itemID, type = ItemTriggerTermType.ITEM) {
    /**
     * The last known **manually set** value known by trigg.
     * This doesn't mean that this reflects the true value in **ANY WAY**.
     *
     * ## Behavior
     *
     * The only assumption with [this][lastKnownValue] is that
     * the value does not get mutated in a way that trigg can't see it.
     *
     * For example if you [set the value][setValue] of this [GDValue][ItemGDValue]
     * then [lastKnownValue] will get modified.
     *
     * However, if you [add] a value to this [GDValue][ItemGDValue],
     * [lastKnownValue] will not change unless it is **non-null**.
     */
    var lastKnownValue: Int? = null

    context(ctx: ObjectManager<SimpleObject>)
    override fun createCounter(pos: Position): CounterLabelTrigger =
        ctx.add { CounterLabelTrigger(pos, this.getItemID(ctx)) }

    /**
     * Sets the value of this [GDValue][WithIdGDValue] when this executes.
     * The modifiers applied to this [GDValue][WithIdGDValue] will
     * be applied to the given [value]
     * @param value The value to set this value to.
     *              No matter what geometry dash will **floor** the result into an [Int] unless
     *              the [roundingType] is changed
     */
    fun setValue(ctx: ObjectManager<SimpleObject>, value: Float): ItemEditTrigger {
        this.lastKnownValue = value.toInt()
        return ctx.add {
            ItemEditTrigger(ctx.getNextPos()).apply {
                resultItemID.value = this@ItemGDValue.getItemID(ctx)
                secondTermFactor.value = value

                firstTermAbsNeg.value = this@ItemGDValue.absNegType
                firstTermRoundingType.value = this@ItemGDValue.roundingType
            }
        }
    }

    /**
     * Sets the value of this [GDValue][WithIdGDValue] when this executes.
     * The modifiers applied to this [GDValue][WithIdGDValue] will
     * be applied to the given [value]
     * @param value The value to set this value to
     */
    fun setValue(ctx: ObjectManager<SimpleObject>, value: Int): ItemEditTrigger =
        this.setValue(ctx, value.toFloat())

    /**
     * Sets the value of this [GDValue][WithIdGDValue] when this executes.
     * The modifiers applied to this [GDValue][WithIdGDValue] will
     * be applied to the given [value]
     * @param value The value to set this value to
     */
    fun setValue(ctx: ObjectManager<SimpleObject>, value: UInt) =
        this.setValue(ctx, value.toInt())
}

class TimerGDValue(timerID: UInt) : WithIdGDValue(timerID, type = ItemTriggerTermType.TIMER) {
    context(ctx: ObjectManager<SimpleObject>)
    override fun createCounter(pos: Position): CounterLabelTrigger =
        ctx.add {
            CounterLabelTrigger(pos, this.getItemID(ctx)).apply {
                timeCounter.value = true
            }
        }
}

class PointsGDValue : GDValue(type = ItemTriggerTermType.POINTS, isValueGlobal = true), ValueWithCounter {
    context(ctx: ObjectManager<SimpleObject>)
    override fun createCounter(pos: Position): CounterLabelTrigger =
        ctx.add {
            CounterLabelTrigger(pos).apply {
                labelType.value = LabelType.POINTS
            }
        }
}

class GameTimeGDValue : GDValue(type = ItemTriggerTermType.TIME, isValueGlobal = true), ValueWithCounter {
    context(ctx: ObjectManager<SimpleObject>)
    override fun createCounter(pos: Position): CounterLabelTrigger =
        ctx.add {
            CounterLabelTrigger(pos).apply {
                labelType.value = LabelType.MAIN_TIME
            }
        }
}

class AttemptGDValue : GDValue(type = ItemTriggerTermType.ATTEMPTS, isValueGlobal = true), ValueWithCounter {
    context(ctx: ObjectManager<SimpleObject>)
    override fun createCounter(pos: Position): CounterLabelTrigger =
        ctx.add {
            CounterLabelTrigger(pos).apply {
                labelType.value = LabelType.ATTEMPTS
            }
        }
}

/**
 * See [ItemGDValue.setValue] for more info
 */
context(ctx: ObjectManager<SimpleObject>)
fun ItemGDValue.setValue(value: Float) =
    this.setValue(ctx, value)

/**
 * See [ItemGDValue.setValue] for more info
 */
context(ctx: ObjectManager<SimpleObject>)
fun ItemGDValue.setValue(value: Int) =
    this.setValue(ctx, value)

/**
 * See [ItemGDValue.setValue] for more info
 */
context(ctx: ObjectManager<SimpleObject>)
fun ItemGDValue.setValue(value: UInt) =
    this.setValue(ctx, value)

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
 * @param setValue if the value should be set inside Geometry Dash has well.
 *                 If set to `null` then another behavior occurs.
 *                 If [value] is equal to `0` (aka the default value for a GDValue)
 *                 then it won't set this [GDValue][ItemGDValue]'s value
 * @see ItemGDValue.setValue
 */
fun ObjectManager<SimpleObject>.item(itemID: UInt? = null, value: Float, setValue: Boolean? = null): ItemGDValue {
    val gdValue = item(itemID)
    if (setValue == true) {
        gdValue.setValue(this, value)
    } else if (setValue == false) {
        gdValue.lastKnownValue = gdValue.applyModifiers(value).toInt()
    } else {
        // Nullable boolean ?
        // This is just like dreamberd !!
        // True, false and maybe !
        if (value == 0f) {
            gdValue.lastKnownValue = 0
        } else {
            gdValue.setValue(this, value)
        }
    }

    return gdValue
}

/**
 * Creates an [ItemGDValue] with an optional itemID and a value
 * @param itemID the optional itemID to set for the [ItemGDValue].
 *               If `null`, a new itemID will get [acquired][GroupHandler.acquireLabel]
 * @param value the value to set
 * @see ItemGDValue.setValue
 */
fun ObjectManager<SimpleObject>.item(itemID: UInt? = null, value: Int): ItemGDValue =
    this.item(itemID, value.toFloat())

/**
 * Creates an [ItemGDValue] with an optional itemID and a value
 * @param itemID the optional itemID to set for the [ItemGDValue].
 *               If `null`, a new itemID will get [acquired][GroupHandler.acquireLabel]
 * @param value the value to set
 * @see ItemGDValue.setValue
 */
fun ObjectManager<SimpleObject>.item(itemID: UInt? = null, value: UInt): ItemGDValue =
    this.item(itemID, value.toFloat())

/**
 * Creates an [TimerGDValue] with a timerID
 * @param timerID the **non-optional** timerID to set for the [TimerGDValue]
 */
fun timer(timerID: UInt) = TimerGDValue(timerID)

fun ItemCompareComparisonType.opposite(): ItemCompareComparisonType =
    when (this) {
        ItemCompareComparisonType.EQUAL -> ItemCompareComparisonType.NOT_EQUAL
        ItemCompareComparisonType.GREATER_THAN -> ItemCompareComparisonType.LESS_THAN_EQUAL
        ItemCompareComparisonType.GREATER_THAN_EQUAL -> ItemCompareComparisonType.LESS_THAN
        ItemCompareComparisonType.LESS_THAN -> ItemCompareComparisonType.GREATER_THAN_EQUAL
        ItemCompareComparisonType.LESS_THAN_EQUAL -> ItemCompareComparisonType.GREATER_THAN
        ItemCompareComparisonType.NOT_EQUAL -> ItemCompareComparisonType.EQUAL
    }

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
 * @param value the value to store
 * @return the created [IntGDValue] from the given [value]
 * @see constFloat
 */
fun const(value: Int) =
    IntGDValue(value)

/**
 * Creates a [IntGDValue] from a constant number.
 * @param value the value to store
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
 * @param value the value to store
 * @return the created [FloatGDValue] from the given [value]
 * @see const
 */
fun constFloat(value: Float) =
    FloatGDValue(value)

/**
 * Creates a [IntGDValue] from a constant **boolean** value.
 * It is stored as either a `0` or a `1` depending on the
 * given [value]
 *
 * @param value the value to store
 * @return the created [IntGDValue] from the given [value]
 * @see const
 */
fun const(value: Boolean) =
    IntGDValue(value.toInt())


fun <Val : GDValue> negate(gdValue: Val): Val =
    gdValue.apply {
        absNegType = ItemTriggerAbsNeg.NEGATIVE
    }

fun <Val : GDValue> abs(gdValue: Val): Val =
    gdValue.apply {
        absNegType = ItemTriggerAbsNeg.ABS
    }


fun <Val : GDValue> round(gdValue: Val): Val =
    gdValue.apply {
        roundingType = ItemTriggerRoundingType.ROUND
    }

fun <Val : GDValue> floor(gdValue: Val): Val =
    gdValue.apply {
        roundingType = ItemTriggerRoundingType.FLOOR
    }

fun <Val : GDValue> ceil(gdValue: Val): Val =
    gdValue.apply {
        roundingType = ItemTriggerRoundingType.CEIL
    }
