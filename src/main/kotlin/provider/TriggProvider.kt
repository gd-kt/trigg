package provider

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
import editor.wsliveeditor.WSLiveEditor
import generator.GroupAdder
import generator.group

/**
 * The base class for everything trigg related.
 * Only 1 instance can be created at the same time. For example this is invalid and will throw an exception:
 * ```kotlin
 * trigg {
 *     trigg {
 *         // ...
 *     }
 *
 *     // ...
 * }
 * ```
 */
class TriggProvider internal constructor(): ObjectManager<SimpleObject>, RawStringable {
    companion object {
        internal var INSTANCE: TriggProvider? = null

        /**
         * Gets the current [TriggProvider] instance
         * @throws IllegalStateException if there is no trigg
         */
        fun getInstance(): TriggProvider =
            checkNotNull(INSTANCE) { "No trigg instance was created ! Maybe you're calling it after it got destroyed ?" }
    }

    init {
        require(INSTANCE == null) {
            "Cannot create more than 1 trigg instance at the same time !"
        }
    }

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
        require(this._init == null) { "Cannot set twice the init block of a TriggProvider" }

        val triggInit = MutableTriggInit()
        initGetter(triggInit)

        this._init = triggInit.toImmutable()
    }

    override fun <T: SimpleObject> add(obj: T): T {
        this.addedObjects.add(obj)
        obj.groups.add(this.init.commonGroup)

        return obj
    }

    override fun spawn(spawner: ObjectManager<SimpleObject>.() -> Unit, delay: Float) {
        val acquiredGroup = this.acquireGroup()
        this.add {
            val spawnTrigger = SpawnTrigger(this.getNextPos(), acquiredGroup, delay)
            spawnTrigger.setSpawned()

            return@add spawnTrigger
        }

        val collector = GroupAdder(this, acquiredGroup)
        collector.spawner()
    }

    /**
     * Spawns the group representing the [entrypoint][dsltypes.init.space.GroupSpace.entrypoint]
     */
    fun executeEntrypoint(delay: Float = 0f): SpawnTrigger =
        this.spawn(this.init.groupSpace.entrypoint, delay).apply {
            spawnTriggered.resetValue()
            multiTriggered.resetValue()

            // We offset the entrypoint trigger a bit to the right
            // to prevent issues where some features
            // don't work when the trigger
            // is outside the "play area"
            pos = pos.copy(x = 0.001f)
        }

    /**
     * Defines the content of the entrypoint
     * @param execute if the entrypoint should
     *                get executed as soon as the level started.
     *                To define your own behavior you can call [executeEntrypoint]
     * @param entrypointSpawner the lambda that will get spawned as the entrypoint
     * @see executeEntrypoint
     */
    fun entrypoint(execute: Boolean = true, entrypointSpawner: ObjectManager<SimpleObject>.() -> Unit) {
        group(this.init.groupSpace.entrypoint.toUInt(), entrypointSpawner)

        if (execute)
            this.executeEntrypoint()
    }

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

    TriggProvider.INSTANCE = trigg
    triggEntryPoint(trigg)
    TriggProvider.INSTANCE = null

    if (trigg.init.clearPreviousObjects)
        WSLiveEditor().use {
            it.removeObjects(trigg.init.commonGroup)
        }

    return trigg.asRawString()
}
