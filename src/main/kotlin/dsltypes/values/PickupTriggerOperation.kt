package dsltypes.values

import editor.objects.SimpleObject
import editor.objects.triggers.counters.PickupOperationType
import editor.objects.triggers.counters.PickupTrigger
import editor.rawstring.property.NULL_GROUP
import editor.rawstring.property.getOrThrow
import generator.GroupAdder
import provider.ObjectManager

internal data class PickupTriggerOperation(val operationType: PickupOperationType, val gdValue: WithIdGDValue, val toEvaluateWith: Float) {
    companion object {
        fun of(pickupTrigger: PickupTrigger): PickupTriggerOperation =
            PickupTriggerOperation(
                pickupTrigger.operationType.getOrThrow(),
                item(pickupTrigger.itemID.getOrThrow()),
                if (pickupTrigger.operationType.value == PickupOperationType.ADDITION) {
                    pickupTrigger.count.getOrThrow().toFloat()
                } else {
                    pickupTrigger.divisionMultiplicationFactor.getOrThrow()
                }
            )

        fun get(ctx: ObjectManager<SimpleObject>, pickupTriggerGetter: ObjectManager<SimpleObject>.() -> Unit): PickupTriggerOperation {
            val groupAdder = GroupAdder(ctx, NULL_GROUP, addObjectsToContext = false)
            pickupTriggerGetter(groupAdder)

            check(groupAdder.collectedObjects.size == 1) { "Cannot get a PickupTrigger if there is not exactly 1 object" }
            check(groupAdder.collectedObjects.filterIsInstance<PickupTrigger>().size == 1) { "Cannot get a PickupTrigger if there is not exactly 1 PickupTrigger" }

            return of(groupAdder.collectedObjects.filterIsInstance<PickupTrigger>().first())
        }
    }

    fun createPickupTrigger(ctx: ObjectManager<in PickupTrigger>): PickupTrigger {
        if (this.gdValue is ItemGDValue && this.gdValue.lastKnownValue != null) {
            when (this.operationType) {
                PickupOperationType.ADDITION -> this.gdValue.lastKnownValue = this.gdValue.lastKnownValue!! + this.toEvaluateWith.toInt()
                PickupOperationType.MULTIPLICATION -> this.gdValue.lastKnownValue = this.gdValue.lastKnownValue!! * this.toEvaluateWith.toInt()
                PickupOperationType.DIVISION -> this.gdValue.lastKnownValue = this.gdValue.lastKnownValue!! / this.toEvaluateWith.toInt()
            }
        }

        return ctx.add {
            PickupTrigger(ctx.getNextPos(), this.gdValue.getItemID(ctx)).apply {
                operationType.value = this@PickupTriggerOperation.operationType
                if (this@PickupTriggerOperation.operationType == PickupOperationType.ADDITION) {
                    count.value = this@PickupTriggerOperation.toEvaluateWith.toInt()
                } else {
                    divisionMultiplicationFactor.value = this@PickupTriggerOperation.toEvaluateWith
                }
            }
        }
    }
}

/**
 * Adds [value] to this [GDValue][ItemGDValue]
 * @param ctx the context in which to add the [PickupTrigger]
 * @param value the value to **add** to this [GDValue][ItemGDValue]
 * @return The [PickupTrigger] used to add [value] to this [GDValue][ItemGDValue]
 */
fun ItemGDValue.add(ctx: ObjectManager<in PickupTrigger>, value: Int): PickupTrigger =
    PickupTriggerOperation(PickupOperationType.ADDITION, this, value.toFloat()).createPickupTrigger(ctx)

/**
 * Adds [value] to this [GDValue][ItemGDValue]
 * @param ctx the context in which to add the [PickupTrigger]
 * @param value the value to **add** to this [GDValue][ItemGDValue]
 * @return The [PickupTrigger] used to add [value] to this [GDValue][ItemGDValue]
 */
context(ctx: ObjectManager<in PickupTrigger>)
fun ItemGDValue.add(value: Int): PickupTrigger =
    this.add(ctx, value)


/**
 * Multiplies [value] by this [GDValue][ItemGDValue]
 * @param ctx the context in which to add the [PickupTrigger]
 * @param value the value to **multiply** by this [GDValue][ItemGDValue]
 * @return The [PickupTrigger] used to multiply [value] by this [GDValue][ItemGDValue]
 */
fun ItemGDValue.multiply(ctx: ObjectManager<in PickupTrigger>, value: Float): PickupTrigger =
    PickupTriggerOperation(PickupOperationType.MULTIPLICATION, this, value).createPickupTrigger(ctx)

/**
 * Multiplies [value] by this [GDValue][ItemGDValue]
 * @param ctx the context in which to add the [PickupTrigger]
 * @param value the value to **multiply** by this [GDValue][ItemGDValue]
 * @return The [PickupTrigger] used to multiply [value] by this [GDValue][ItemGDValue]
 */
context(ctx: ObjectManager<in PickupTrigger>)
fun ItemGDValue.multiply(value: Float): PickupTrigger =
    this.multiply(ctx, value)


/**
 * Divides [value] by this [GDValue][ItemGDValue]
 * @param ctx the context in which to add the [PickupTrigger]
 * @param value the value to **divide** by this [GDValue][ItemGDValue]
 * @return The [PickupTrigger] used to divide [value] by this [GDValue][ItemGDValue]
 */
fun ItemGDValue.divide(ctx: ObjectManager<in PickupTrigger>, value: Float): PickupTrigger =
    PickupTriggerOperation(PickupOperationType.DIVISION, this, value).createPickupTrigger(ctx)

/**
 * Divides [value] by this [GDValue][ItemGDValue]
 * @param ctx the context in which to add the [PickupTrigger]
 * @param value the value to **divide** by this [GDValue][ItemGDValue]
 * @return The [PickupTrigger] used to divide [value] by this [GDValue][ItemGDValue]
 */
context(ctx: ObjectManager<in PickupTrigger>)
fun ItemGDValue.divide(value: Float): PickupTrigger =
    this.divide(ctx, value)
