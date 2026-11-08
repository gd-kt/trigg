package provider

import Group
import Group.Companion.group
import TriggDsl
import editor.objects.GenericGdObject
import editor.objects.SimpleObject
import editor.objects.data.Pos
import editor.objects.triggers.misc.SpawnTrigger
import editor.objects.triggers.setSpawned
import editor.rawstring.property.NULL_GROUP
import generator.GroupAdder
import generator.TriggFunction
import generator.TriggFunctionType
import provider.TriggProvider.TriggFunctionState
import utils.toFloat
import kotlin.collections.set
import kotlin.time.Duration
import kotlin.time.DurationUnit

@TriggDsl
interface ScopedObjectAdder<in Obj : GenericGdObject> {
    /**
     * The group to assign to all the objects in this [ScopedObjectAdder].
     * It is to the implementation to decide is this should get used.
     *
     * By default, it returns [NULL_GROUP], meaning no group will actually get assigned.
     */
    val group: UInt
        get() = NULL_GROUP

    /**
     * Adds at "compile time" an object to the final output.
     * If the object is a trigger, it will be set as [spawned][editor.objects.triggers.TriggerObject.setSpawned]
     * and [multi triggered][editor.objects.triggers.TriggerObject.multiTriggered]
     * @return the created object. It is modifiable
     */
    fun <T : Obj> add(objectGetter: () -> T): T =
        this.add(objectGetter())

    /**
     * Adds at "compile time" an object to the final output.
     * If [obj] is a trigger, it will be set as [spawned][editor.objects.triggers.TriggerObject.setSpawned]
     * and [multi triggered][editor.objects.triggers.TriggerObject.multiTriggered]
     * @return the created object. It is modifiable
     */
    fun <T : Obj> add(obj: T): T

    /**
     * Adds at "compile time" objects to the final output.
     * If any the object is a trigger, it will be set as [spawned][editor.objects.triggers.TriggerObject.setSpawned]
     * and [multi triggered][editor.objects.triggers.TriggerObject.multiTriggered]
     */
    fun add(vararg objs: Obj) =
        this.add(objs.toList())

    /**
     * Adds at "compile time" objects to the final output.
     * If any of the object is a trigger, it will be set as [spawned][editor.objects.triggers.TriggerObject.setSpawned]
     * and [multi triggered][editor.objects.triggers.TriggerObject.multiTriggered]
     */
    fun add(objs: Iterable<Obj>) =
        objs.forEach(::add)

    /**
     * Spawns a lambda after a given delay (which is `>=0`)
     * @param group the group of the content of what tp spawn.
     *              If `null`, a new group will get [acquired][GroupHandler.acquireGroup] instead
     * @param delay the time after which the [lambda][spawner] will get spawned. It must be `>=0`
     * @param spawner what to spawn
     * @return the created [SpawnTrigger] used to spawn
     * @see spawnFunction
     */
    fun spawn(group: Group? = null, delay: Float = 0f, spawner: ObjectManager<Obj>.() -> Unit): SpawnTrigger

    /**
     * Defines a [TriggFunction] to the **global [trigg instance][TriggProvider]**.
     * To call it you can then use [spawnFunction].
     *
     * It is defined like so:
     * ```kotlin
     * @TriggFunction
     * fun myFunc(ctx: ScopedObjectAdder<SimpleObject>) {
     *    ctx.apply {
     *        add { ColorTrigger(getNextPos(), Color.GREEN, SpecialColors.BACKGROUND.colorID) }
     *        // ...
     *    }
     * }
     *
     * // It works on any ScopedObjectAdder<*> impl
     * trigg {
     *     defineFunction(::myFunc)
     * }
     * ```
     *
     * @param triggFunction the function to define
     * @return the group of the function
     * @see spawnFunction
     * @see spawn
     * @see TriggFunction
     */
    fun defineFunction(triggFunction: TriggFunctionType): UInt

    /**
     * Spawns a [TriggFunction] stored in the **global [trigg instance][TriggProvider]**.
     *
     * You can spawn a [TriggFunction] like so:
     *
     * ```kotlin
     * @TriggFunction
     * fun myFunc(ctx: ScopedObjectAdder<SimpleObject>) {
     *    ctx.apply {
     *        add { ColorTrigger(getNextPos(), Color.GREEN, SpecialColors.BACKGROUND.colorID) }
     *        // ...
     *    }
     * }
     *
     * trigg {
     *     defineFunction(::myFunc)
     *
     *     entrypoint {
     *         // It works on any ScopedObjectAdder<*> impl
     *         spawnFunction(::myFunc)
     *
     *         // You can also add some delay
     *         spawnFunction(::myFunc, delay = 1.75f)
     *     }
     * }
     * ```
     *
     * Although, if [canSpawnEarly] is `false` and you spawn the given [TriggFunction] without defining it,
     * an [IllegalStateException] exception will be thrown.
     * @param triggFunction the function to spawn
     * @param canSpawnEarly if the function can be spawned even if it has not been defined yet
     * @param delay the time it takes to execute the given [function][triggFunction] after executing this
     * @see defineFunction
     * @see TriggFunction
     */
    fun spawnFunction(triggFunction: TriggFunctionType, canSpawnEarly: Boolean = true, delay: Float = 0f): SpawnTrigger

    /**
     * Gets the ID of a [TriggFunction].
     * If it has not been called or defined yet a new group
     * will be [acquired][GroupHandler.acquireGroup]
     * @see spawnFunction
     * @see defineFunction
     */
    fun getFunctionID(triggFunction: TriggFunctionType): UInt // TODO: Add test

    /**
     * Gets the pos to place a trigger.
     * This is not needed but is more for not
     * cluttering every objects around a single point
     * @return the pos where to place the next object
     */
    fun getNextPos(): Pos
}

/**
 * Spawns a specific group
 * @param group the group to spawn
 * @param delay the time after which the [group] will get spawned. It must be `>=0`
 * @return the created [SpawnTrigger]
 */
fun ScopedObjectAdder<SpawnTrigger>.spawn(group: Group, delay: Float = 0f): SpawnTrigger =
    this.add {
        SpawnTrigger(this.getNextPos(), group.value, delay)
    }

/**
 * Spawns a specific group
 * @param group the group to spawn
 * @param delay the time after which the [group] will get spawned. It must be `>=0`
 * @return the created [SpawnTrigger]
 */
fun ScopedObjectAdder<SpawnTrigger>.spawn(group: Group, delay: Duration): SpawnTrigger =
    this.spawn(group, delay.toFloat(DurationUnit.SECONDS))

/**
 * Spawns a lambda after a given delay (which is `>=0`)
 * @param group the group of the content of what tp spawn.
 *              If `null`, a new group will get [acquired][GroupHandler.acquireGroup] instead
 * @param delay the time after which the [lambda][spawner] will get spawned. It must be `>=0`
 * @param spawner what to spawn
 * @return the created [SpawnTrigger] used to spawn
 * @see spawnFunction
 */
fun <T : GenericGdObject> ScopedObjectAdder<T>.spawn(group: Group? = null, delay: Duration, spawner: ObjectManager<T>.() -> Unit) =
    this.spawn(group, delay.toFloat(DurationUnit.SECONDS), spawner)

/**
 * Spawns a [TriggFunction] stored in the **global [trigg instance][TriggProvider]**.
 *
 * See [ScopedObjectAdder.spawnFunction] for more info
 * @param triggFunction the function to spawn
 * @param canSpawnEarly if the function can be spawned even if it has not been defined yet
 * @param delay the time it takes to execute the given [function][triggFunction] after executing this
 */
fun ScopedObjectAdder<*>.spawnFunction(triggFunction: TriggFunctionType, canSpawnEarly: Boolean = true, delay: Duration): SpawnTrigger =
    this.spawnFunction(triggFunction, canSpawnEarly, delay.toFloat(DurationUnit.SECONDS))

fun ScopedObjectAdder<SimpleObject>.add(groupAdder: GroupAdder) =
    this.add(groupAdder.collectedObjects)

internal fun ObjectManager<SimpleObject>.defineFunctionHelper(functionContent: TriggFunctionType, triggFunctions: MutableMap<TriggFunctionType, TriggFunctionState>): UInt {
    TriggFunction.validate(functionContent)

    if (triggFunctions.contains(functionContent))
        check(!triggFunctions[functionContent]!!.hasBeenDefined) { "Cannot redefine a TriggFunction" }

    val acquiredGroup = triggFunctions[functionContent]?.group ?: this.acquireGroup().group

    functionContent.call(GroupAdder(this, acquiredGroup.value))
    triggFunctions[functionContent] = TriggFunctionState(acquiredGroup, hasBeenDefined = true)

    return acquiredGroup.value
}

internal fun ObjectManager<SimpleObject>.spawnFunctionHelper(function: TriggFunctionType, canSpawnEarly: Boolean, delay: Float, triggFunctions: MutableMap<TriggFunctionType, TriggFunctionState>): SpawnTrigger {
    if (canSpawnEarly && !triggFunctions.contains(function)) {
        TriggFunction.validate(function)

        val acquiredGroup = this.acquireGroup()
        triggFunctions[function] = TriggFunctionState(acquiredGroup.group, hasBeenDefined = false)

        return this.spawn(acquiredGroup.group, delay)
    } else {
        check(triggFunctions.contains(function)) { "Cannot spawn a function if it has not been defined yet" }
        return this.spawn(triggFunctions[function]!!.group, delay)
    }
}

internal fun ObjectManager<SimpleObject>.getFunctionIdHelper(function: TriggFunctionType, triggFunctions: MutableMap<TriggFunctionType, TriggFunctionState>): UInt {
    if (triggFunctions.contains(function)) {
        return triggFunctions[function]!!.group.value
    } else {
        TriggFunction.validate(function)

        val acquiredGroup = this.acquireGroup()
        triggFunctions[function] = TriggFunctionState(acquiredGroup.group, hasBeenDefined = false)

        return acquiredGroup
    }
}
