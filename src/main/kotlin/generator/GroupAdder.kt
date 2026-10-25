package generator

import editor.objects.SimpleObject
import editor.objects.data.Pos
import editor.objects.triggers.TriggerObject
import editor.objects.triggers.misc.SpawnTrigger
import editor.objects.triggers.setSpawned
import editor.rawstring.property.getOrThrow
import provider.ObjectManager
import provider.TriggProvider
import kotlin.collections.first

/**
 * Collects objects which are all assigned a default group
 * @property group the group to assign
 * @property addObjectsToContext if the objects should get automatically added to the [context]
 */
class GroupAdder(
    private val context: ObjectManager<SimpleObject>,
    val group: UInt = 0u,
    val addObjectsToContext: Boolean = true
): ObjectManager<SimpleObject> {
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

    override fun spawn(spawner: ObjectManager<SimpleObject>.() -> Unit, delay: Float): SpawnTrigger {
        val acquiredGroup = this.context.acquireGroup()

        val collector = GroupAdder(this.context, acquiredGroup, addObjectsToContext = this.addObjectsToContext)
        collector.spawner()

        // We don't use ScopedObjectAdded#add because
        // it adds a group
        this.collectedObjects.addAll(collector.collectedObjects)
        this.spawnBranches.add(collector.collectedObjects)

        return this.addSpawnTrigger(acquiredGroup, delay)
    }

    override fun getNextPos(): Pos = this.context.getNextPos()

    private fun addSpawnTrigger(targetID: UInt, delay: Float): SpawnTrigger =
        this.add {
            SpawnTrigger(this.context.getNextPos(), targetID, delay)
        }

    override val nextFreeGroup: UInt
        get() = this.context.nextFreeGroup

    override val nextFreeLabel: UInt
        get() = this.context.nextFreeLabel

    override fun acquireGroup(): UInt =
        this.context.acquireGroup()

    override fun acquireLabel(): UInt =
        this.context.acquireLabel()
}

// TODO: Tests for all "group" functions

/**
 * Creates a [GroupAdder] to create objects at "compile time" with a given group.
 * If there are spawn branches, these won't get assigned the given [group], only
 * the root objects will.
 * @param group the group to add to all the collected objects
 * @param groupAdder the lambda used to collected all the objects
 * @see ObjectManager.groups
 * @see onlyGroup
 */
fun ObjectManager<SimpleObject>.group(group: UInt, groupAdder: GroupAdder.() -> Unit) =
    GroupAdder(this, group).apply(groupAdder)

/**
 * Creates a [GroupAdder] to create objects at "compile time" with multiple groups.
 * If there are spawn branches, these won't get assigned the given [group], only
 * the root objects will.
 * @param groups the groups to add to all the collected objects
 * @param groupAdder the lambda used to collected all the objects
 * @see ObjectManager.group
 * @see onlyGroup
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun ObjectManager<SimpleObject>.groups(vararg groups: UInt, groupAdder: GroupAdder.() -> Unit): GroupAdder {
    val myGroupAdder = GroupAdder(this, groups.first()).apply(groupAdder)

    myGroupAdder.collectedObjects.forEach { obj ->
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
 * @see onlyGroups
 * @see ObjectManager.group
 */
fun onlyGroup(group: UInt, groupAdder: GroupAdder.() -> Unit): GroupAdder {
    val myGroupAdder = GroupAdder(TriggProvider.getInstance(), ONLY_GROUP_TEMP_GROUP).apply(groupAdder)
    myGroupAdder.collectedObjects.forEach {
        if (it.groups.getOrThrow().contains(ONLY_GROUP_TEMP_GROUP)) {
            it.groups.remove(ONLY_GROUP_TEMP_GROUP)
            it.groups.add(group)
        }
    }

    return myGroupAdder
}

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
 * @see onlyGroup
 * @see ObjectManager.group
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun onlyGroups(vararg groups: UInt, groupAdder: GroupAdder.() -> Unit): GroupAdder {
    val myGroupAdder = GroupAdder(TriggProvider.getInstance(), 0u).apply(groupAdder)
    myGroupAdder.collectedObjects.forEach {
        if (it.groups.getOrThrow().contains(ONLY_GROUP_TEMP_GROUP)) {
            it.groups.remove(ONLY_GROUP_TEMP_GROUP)
            it.groups.getOrCreateCollection().addAll(groups)
        }
    }

    return myGroupAdder
}
