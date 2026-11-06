package generator

import Group
import Group.Companion.group
import editor.objects.SimpleObject
import editor.objects.data.GridPos
import editor.objects.data.Pos
import editor.objects.triggers.TriggerObject
import editor.objects.triggers.misc.SpawnTrigger
import editor.objects.triggers.setSpawned
import editor.rawstring.property.NULL_GROUP
import editor.rawstring.property.getOrThrow
import provider.ObjectManager
import provider.TriggProvider
import provider.defineFunctionHelper
import provider.spawnFunctionHelper
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Collects objects which are all assigned a default group
 * @property context the context in which to add the objects
 * @property group the group to assign
 * @property addObjectsToContext if the objects should get automatically added to the [context]
 */
class GroupAdder(
    private val context: ObjectManager<SimpleObject>,
    val group: UInt = NULL_GROUP,
    val addObjectsToContext: Boolean = true
): ObjectManager<SimpleObject> {
    init {
        if (this.context is TriggProvider)
            this.context.throwIfFinished()
    }

    /**
     * The objects that were collected.
     * To add objects, you must do it through the [add] functions
     */
    val collectedObjects: List<SimpleObject>
        field: MutableList<SimpleObject> = mutableListOf()

    /**
     * The spawn triggers that branch off this [GroupAdder].
     * To add "spawn branches" you can call [spawn]
     */
    val spawnBranches: List<List<SimpleObject>>
        field: MutableList<MutableList<SimpleObject>> = mutableListOf()

    override fun <T : SimpleObject> add(obj: T): T {
        obj.groups.add(this.group)
        this.collectedObjects.add(obj)

        if (obj is TriggerObject)
            obj.setSpawned(multiTriggered = true)

        if (this.addObjectsToContext)
            this.context.add(obj)

        return obj
    }

    override fun spawn(group: Group?, delay: Float, spawner: ObjectManager<SimpleObject>.() -> Unit): SpawnTrigger {
        val groupUInt = group?.let { group.value }
        val acquiredGroup = groupUInt ?: this.acquireGroup()

        val collector = GroupAdder(this.context, acquiredGroup, addObjectsToContext = this.addObjectsToContext)
        collector.spawner()

        // We don't use ScopedObjectAdded#add because
        // it adds a group
        this.collectedObjects.addAll(collector.collectedObjects)
        this.spawnBranches.add(collector.collectedObjects)

        return this.addSpawnTrigger(acquiredGroup, delay)
    }

    override fun defineFunction(triggFunction: TriggFunctionType): UInt =
        this.defineFunctionHelper(triggFunction, TriggProvider.getInstance()._triggFunctions)

    override fun spawnFunction(triggFunction: TriggFunctionType, canSpawnEarly: Boolean, delay: Float): SpawnTrigger =
        this.spawnFunctionHelper(triggFunction, canSpawnEarly, delay, TriggProvider.getInstance()._triggFunctions)

    override fun getNextPos(): Pos = this.context.getNextPos() + GridPos(2.0f, 0f)

    private fun addSpawnTrigger(targetID: UInt, delay: Float): SpawnTrigger =
        this.add {
            SpawnTrigger(this.context.getNextPos(), targetID, delay)
        }

    override val nextFreeGroup: UInt
        get() = this.context.nextFreeGroup

    override val nextFreeLabel: UInt
        get() = this.context.nextFreeLabel

    override val acquiredRegisters: Set<UInt>
        get() = this.context.acquiredRegisters

    override fun acquireGroup(): UInt =
        this.context.acquireGroup()

    override fun acquireLabel(): UInt =
        this.context.acquireLabel()

    override fun acquireRegister(): UInt =
        this.context.acquireRegister()

    override fun releaseRegister(register: UInt) =
        this.context.releaseRegister(register)
}

    /**
 * Creates a [GroupAdder] to create objects at "compile time" with a given group.
 * If there are spawn branches, these won't get assigned the given [group], only
 * the root objects will.
 * @param group the group to add to all the collected objects
 * @param groupAdder the lambda used to collected all the objects
 * @see ObjectManager.groups
 * @see onlyGroup
 */
@OptIn(ExperimentalContracts::class)
inline fun ObjectManager<SimpleObject>.group(group: UInt, groupAdder: GroupAdder.() -> Unit): GroupAdder {
    contract {
        callsInPlace(groupAdder, InvocationKind.EXACTLY_ONCE)
    }

    return GroupAdder(this, group).apply(groupAdder)
}

/**
 * Creates a [GroupAdder] to create objects at "compile time" with multiple groups.
 * If there are spawn branches, these won't get assigned the given [group], only
 * the root objects will.
 * @param groups the groups to add to all the collected objects
 * @param groupAdder the lambda used to collected all the objects
 * @return the [GroupAdder] used to collect the objects
 * @see ObjectManager.group
 * @see onlyGroup
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalUnsignedTypes
fun ObjectManager<SimpleObject>.groups(vararg groups: UInt, groupAdder: GroupAdder.() -> Unit): GroupAdder {
    contract {
        callsInPlace(groupAdder, InvocationKind.EXACTLY_ONCE)
    }

    val myGroupAdder = GroupAdder(this, groups.first()).apply(groupAdder)

    myGroupAdder.collectedObjects.forEach { obj ->
        // This checks prevents objects coming from
        // spawn branches to get the given groups
        if (obj.groups.getOrThrow().contains(groups.first()))
            obj.groups.getOrCreateCollection().addAll(groups)
    }

    return myGroupAdder
}

private const val ONLY_GROUP_TEMP_GROUP: UInt = UInt.MAX_VALUE

/**
 * Creates a [GroupAdder] to create objects at "compile time" with a given [group].
 * If there are spawn branches, these won't get assigned the given [group], only
 * the root objects will.
 *
 * Unlike [ObjectManager.group], if for example we're inside a `spawn` statement
 * but want to add objects only with a given group, then this can prove to
 * be useful
 * @param group the group to add to all the collected objects
 * @param groupAdder the lambda used to collected all the objects
 * @return the [GroupAdder] used to collect the objects
 * @see onlyGroups
 * @see ObjectManager.group
 */
@OptIn(ExperimentalContracts::class, ExperimentalUnsignedTypes::class)
fun onlyGroup(group: UInt, groupAdder: GroupAdder.() -> Unit): GroupAdder {
    contract {
        callsInPlace(groupAdder, InvocationKind.EXACTLY_ONCE)
    }

    return onlyGroups(group, groupAdder = groupAdder)
}

/**
 * Adds every object collected in the given [groupAdder] to
 * the root [TriggProvider] instance
 * @param groupAdder the lambda used to collected all the objects
 * @return the [GroupAdder] used to collect the objects
 */
fun addToRoot(groupAdder: GroupAdder.() -> Unit): GroupAdder =
    onlyGroup(NULL_GROUP, groupAdder)

/**
 * Creates a [GroupAdder] to create objects at "compile time" with a given [group].
 * If there are spawn branches, these won't get assigned the given [group], only
 * the root objects will.
 *
 * Unlike [ObjectManager.group], if for example we're inside a `spawn` statement
 * but want to add objects only with a given group, then this can prove to
 * be useful
 * @param groups the groups to add to all the collected objects
 * @param groupAdder the lambda used to collected all the objects
 * @return the [GroupAdder] used to collect the objects
 * @see onlyGroup
 * @see ObjectManager.group
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalUnsignedTypes
fun onlyGroups(vararg groups: UInt, groupAdder: GroupAdder.() -> Unit): GroupAdder {
    contract {
        callsInPlace(groupAdder, InvocationKind.EXACTLY_ONCE)
    }

    val myGroupAdder = GroupAdder(TriggProvider.getInstance(), ONLY_GROUP_TEMP_GROUP).apply(groupAdder)
    myGroupAdder.collectedObjects.forEach {
        // This checks prevents objects coming from
        // spawn branches to get the given groups
        if (it.groups.getOrThrow().contains(ONLY_GROUP_TEMP_GROUP)) {
            it.groups.remove(ONLY_GROUP_TEMP_GROUP)
            it.groups.getOrCreateCollection().addAll(groups)
        }
    }

    return myGroupAdder
}
