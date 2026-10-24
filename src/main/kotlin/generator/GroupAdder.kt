package generator

import editor.objects.SimpleObject
import editor.objects.data.Pos
import editor.objects.triggers.misc.SpawnTrigger
import editor.objects.triggers.setSpawned
import editor.rawstring.property.getOrThrow
import provider.ObjectManager
import provider.TriggProvider
import kotlin.collections.first

/**
 * Collects objects which are all assigned a default group
 * @property group the group to assign
 * @property addObjectsToContext TODO
 */
class GroupAdder(private val context: ObjectManager<SimpleObject>, val group: UInt = 0u, val addObjectsToContext: Boolean = true): ObjectManager<SimpleObject> {
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

    /**
     * Adds at "compile time" an object to the final output with this [GroupAdder]'s [group]
     * @return the created object. It is modifiable
     */
    override fun <T: SimpleObject> add(objectGetter: () -> T): T =
        this.add(objectGetter())

    /**
     * Adds at "compile time" an object to the final output with this [GroupAdder]'s [group]
     * @return the created object. It is modifiable
     */
    override fun <T: SimpleObject> add(obj: T): T {
        obj.groups.add(this.group)
        this.collectedObjects.add(obj)

        if (this.addObjectsToContext)
            this.context.add(obj)

        return obj
    }

    /**
     * Adds at "compile time" objects to the final output having this [GroupAdder]'s [group]
     */
    override fun add(vararg objs: SimpleObject) {
        objs.forEach {
            it.groups.add(this.group)
            this.collectedObjects.add(it)

            if (this.addObjectsToContext)
                this.context.add(it)
        }
    }

    override fun spawn(spawner: ObjectManager<SimpleObject>.() -> Unit, delay: Float) {
        val acquiredGroup = this.context.acquireGroup()

        this.addSpawnTrigger(acquiredGroup, delay)

        val collector = GroupAdder(this.context, acquiredGroup, addObjectsToContext = false)
        collector.spawner()

        // We don't use ScopedObjectAdded#add because
        // it adds a group
        this.collectedObjects.addAll(collector.collectedObjects)
        this.spawnBranches.add(collector.collectedObjects)
    }

    override fun getNextPos(): Pos = this.context.getNextPos()

    private fun addSpawnTrigger(targetID: UInt, delay: Float) {
        this.add {
            val trigg = SpawnTrigger(this.context.getNextPos(), targetID, delay)
            trigg.setSpawned()

            return@add trigg
        }
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

/**
 * Creates a [GroupAdder] to create objects at "compile time" with
 * a given [group]
 * @see GroupAdder.add
 */
fun ObjectManager<SimpleObject>.group(group: UInt, groupAdder: GroupAdder.() -> Unit) =
    GroupAdder(this, group).apply(groupAdder)
