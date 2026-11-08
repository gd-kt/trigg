package provider

import Group.Companion.group
import dsltypes.comparison.comp
import dsltypes.values.ComparisonCreator
import dsltypes.values.ItemGDValue
import dsltypes.values.item
import editor.objects.SimpleObject
import editor.objects.triggers.TriggerObject
import utils.toFloat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val LOOP_ITER_DELAY = (1e-6).seconds

/**
 * Creates a loop which executes every [executeEvery] seconds
 * @param executeEvery the delay after which to spawn every iteration
 * @param executeAfter the delay before spawning the **first** iteration
 * @param loopContent what to execute every [executeEvery] seconds
 * @throws IllegalArgumentException if a duration is equal to `Duration.ZERO`. Geometry Dash cannot loop something every frame
 * @throws IllegalArgumentException if a duration is negative
 */
fun ObjectManager<SimpleObject>.loopDelay(executeEvery: Duration, executeAfter: Duration = 0.seconds, loopContent: ObjectManager<SimpleObject>.() -> Unit) {
    require(executeEvery != Duration.ZERO) { "Cannot have a delayed loop which executes every 0 seconds. Even if you could, Geometry Dash wouldn't be able to loop it every frame" }
    require(executeEvery < Duration.ZERO) { "Cannot have an executeEvery negative delay, but $executeEvery was given" }
    require(executeAfter < Duration.ZERO) { "Cannot have an executeAfter negative delay, but $executeAfter was given" }

    val group = this.acquireGroup()
    this.spawn(group.group, delay = executeAfter.toDouble(DurationUnit.SECONDS).toFloat()) {
        spawn(group.group, executeEvery.toFloat(DurationUnit.SECONDS))
        loopContent(this)
    }
}

/**
 * Creates a `for loop` that will loop over the given [counter]
 * @param counter the counter of the loop
 * @param onIteration the lambda that modifies the counter on each iteration
 * @param compFactory the factory that creates
 * @param iterationDelay the delay between each iteration
 * @param executeAfter the delay before this loop starts
 * @param loopContent the content of the loop
 * @throws IllegalArgumentException if `executeEvery == 0.seconds`. Geometry Dash cannot loop something every frame
 * @throws IllegalArgumentException if a duration is negative
 */
fun ObjectManager<SimpleObject>.forLoop(
    counter: ItemGDValue,
    onIteration: (counter: ItemGDValue) -> TriggerObject,
    compFactory: ObjectManager<SimpleObject>.(counter: ItemGDValue) -> ComparisonCreator,
    iterationDelay: Duration = LOOP_ITER_DELAY,
    executeAfter: Duration = 0.seconds,
    loopContent: LoopObjectManager.(counter: ItemGDValue) -> Unit
) {
    require(iterationDelay != Duration.ZERO) { "Cannot have a delayed loop which executes every 0 seconds. Even if you could, Geometry Dash won't be able to loop it every frame" }
    require(iterationDelay >= Duration.ZERO) { "Cannot have an iterationDelay negative delay, but $iterationDelay was given" }
    require(executeAfter >= Duration.ZERO) { "Cannot have an executeAfter negative delay, but $executeAfter was given" }

    val group = this.acquireGroup().group
    this.spawn(group, delay = executeAfter) {
        comp(compFactory(this, counter)) {
            var onFinishIteration: (ObjectManager<SimpleObject>.() -> Unit)? = null
            then {
                val loopGroupAdder = LoopObjectManager(this)
                loopContent(loopGroupAdder, counter)

                onFinishIteration = loopGroupAdder.onFinish

                spawn(group, delay = iterationDelay)
                onIteration(counter)
            } fail {
                onFinishIteration?.invoke(this)
            }
        }
    }
}

/**
 * Creates a `for loop` that will loop over an acquired counter
 * @param firstValue the first value of the counter. It uses [registers][acquireRegister]
 * @param onIteration the lambda that modifies the counter on each iteration
 * @param compFactory the factory that creates
 * @param iterationDelay the delay between each iteration
 * @param executeAfter the delay before this loop starts
 * @param loopContent the content of the loop
 * @throws IllegalArgumentException if `executeEvery == 0.seconds`. Geometry Dash cannot loop something every frame
 * @throws IllegalArgumentException if a duration is negative
 */
fun ObjectManager<SimpleObject>.forLoop(
    firstValue: Int,
    onIteration: (counter: ItemGDValue) -> TriggerObject,
    compFactory: ObjectManager<SimpleObject>.(counter: ItemGDValue) -> ComparisonCreator,
    iterationDelay: Duration = LOOP_ITER_DELAY,
    executeAfter: Duration = 0.seconds,
    loopContent: LoopObjectManager.(counter: ItemGDValue) -> Unit
) {
    val acquiredRegister = this.acquireRegister()
    this.forLoop(
        item(acquiredRegister, firstValue.toFloat(), true),
        onIteration,
        compFactory,
        iterationDelay, executeAfter,
        loopContent
    )

    acquiredRegister.releaseRegister()
}

/**
 * Creates a `while loop` that will execute until the given [comparison][compFactory] is no longer `true`
 * @param compFactory the factory that creates
 * @param iterationDelay the delay between each iteration
 * @param executeAfter the delay before this loop starts
 * @param loopContent the content of the loop
 * @throws IllegalArgumentException if `executeEvery == 0.seconds`. Geometry Dash cannot loop something every frame
 * @throws IllegalArgumentException if a duration is negative
 */
fun ObjectManager<SimpleObject>.whileLoop(
    compFactory: ObjectManager<SimpleObject>.() -> ComparisonCreator,
    iterationDelay: Duration = LOOP_ITER_DELAY,
    executeAfter: Duration = 0.seconds,
    loopContent: LoopObjectManager.() -> Unit
) {
    require(iterationDelay != Duration.ZERO) { "Cannot have a delayed loop which executes every 0 seconds. Even if you could, Geometry Dash wouldn't be able to loop it every frame" }
    require(iterationDelay >= Duration.ZERO) { "Cannot have an executeEvery negative delay, but $iterationDelay was given" }
    require(executeAfter >= Duration.ZERO) { "Cannot have an executeAfter negative delay, but $executeAfter was given" }

    val group = this.acquireGroup().group
    this.spawn(group, delay = executeAfter) {
        comp(compFactory(this)) {
            var onFinishIteration: (ObjectManager<SimpleObject>.() -> Unit)? = null
            then {
                val loopGroupAdder = LoopObjectManager(this)
                loopContent(loopGroupAdder)

                onFinishIteration = loopGroupAdder.onFinish

                spawn(group, delay = iterationDelay)
            } fail {
                onFinishIteration?.invoke(this)
            }
        }
    }
}


class LoopObjectManager(
    parentObjectManager: ObjectManager<SimpleObject>,
) : WrappedObjectManager<SimpleObject>(parentObjectManager) {
    var onFinish: (ObjectManager<SimpleObject>.() -> Unit)? = null
        private set

    /**
     * Sets the content of what to execute when the loop finishes
     * @param onFinish what to execute when the loop finishes executing
     */
    fun onFinish(onFinish: ObjectManager<SimpleObject>.() -> Unit) {
        this.onFinish = onFinish
    }
}
