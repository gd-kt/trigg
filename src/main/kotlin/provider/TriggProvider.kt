package provider

import Label
import Owner
import generator.GroupAdder
import dsltypes.init.MutableTriggInit
import dsltypes.init.TriggInit
import editor.objects.GenericGdObject
import editor.objects.SimpleObject
import editor.objects.data.GridPos
import editor.objects.data.Pos
import editor.objects.triggers.misc.SpawnTrigger
import editor.objects.triggers.setSpawned
import editor.rawstring.RawStringFactory
import editor.rawstring.RawStringable

class TriggProvider internal constructor(): ObjectManager<SimpleObject>, RawStringable {
    private val addedObjects = mutableListOf<GenericGdObject>()

    private var currentPos: Pos = Pos.gridCentered(-1f, 0f)

    private var _init: TriggInit? = null
    val init: TriggInit
        get() = this._init ?: TriggInit.DEFAULT

    override var nextFreeGroup: UInt = 1u
        get() = field + this.init.groupSpace.baseOffset.value
        private set

    override var nextFreeLabel: UInt = 1u
        get() = field + this.init.labelSpace.registerRange.first
        private set

    override fun asRawString(): String =
        RawStringFactory.joinRawStrings(this.addedObjects)

    /**
     * Creates the initializer for this [TriggProvider]
     */
    fun init(initGetter: MutableTriggInit.() -> Unit) {
        if (this._init == null) {
            val triggInit = MutableTriggInit()
            initGetter(triggInit)

            this._init = triggInit.toImmutable()
        } else {
            error("Cannot set twice the init block of a TriggProvider")
        }
    }

    override fun <T: SimpleObject> add(objectGetter: () -> T): T {
        val obj = objectGetter()
        obj.groups.add(this.init.baseGroup)
        this.addedObjects.add(obj)

        return obj
    }

    override fun <T: SimpleObject> add(obj: T): T {
        this.addedObjects.add(obj)
        obj.groups.add(this.init.baseGroup)
        return obj
    }

    override fun add(vararg objs: SimpleObject) {
        objs.forEach {
            it.groups.add(this.init.baseGroup)
        }

        this.addedObjects.addAll(objs)
    }

    override fun spawn(spawner: ScopedObjectAdder<SimpleObject>.() -> Unit, delay: Float) {
        val acquiredGroup = this.acquireGroup()
        this.add {
            val trigg = SpawnTrigger(this.getNextPos(), acquiredGroup, delay)
            trigg.setSpawned()

            return@add trigg
        }

        val collector = GroupAdder(this, acquiredGroup, addObjectsToContext = false)
        collector.spawner()

        this.add(*collector.collectedObjects.toTypedArray())
    }

    fun createLabel(addVisualConter: Boolean = false): Label = createLabel(addVisualConter, Owner.USER)

    internal fun createLabel(addVisualConter: Boolean = false, owner: Owner): Label =
        Label(this, addVisualConter, owner)

    override fun getNextPos(): Pos {
        val posSnapshot = this.currentPos.copy()
        this.currentPos += GridPos(0f, 1f)
        return posSnapshot + this.init.basePos
    }

    override fun acquireGroup(): UInt {
        if (this.nextFreeGroup !in this.init.groupSpace.groupRange)
            throw IllegalStateException("Too many acquired groups. There are ${this.init.groupSpace.allocatedGroups} allocatable groups yet all of them were used")

        val acquiredGroup = this.nextFreeGroup
        this.nextFreeGroup++

        return acquiredGroup
    }

    override fun acquireLabel(): UInt {
        if (this.nextFreeLabel !in this.init.labelSpace.scriptRange)
            throw IllegalStateException("Too many acquired labels. There are ${this.init.labelSpace.script} allocatable labels yet all of them were used")

        val acquiredGroup = this.nextFreeGroup
        this.nextFreeGroup++

        return acquiredGroup
    }
}

typealias TriggerProviderFunc = TriggProvider.() -> Unit

fun trigg(triggEntryPoint: TriggerProviderFunc): String {
    val trigg = TriggProvider()
    triggEntryPoint(trigg)

    return trigg.asRawString()
}
