package provider

import Group
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
import generator.TriggFunctionType
import generator.group
import java.util.SequencedSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * The base class for everything trigg related.
 * A [TriggProvider] is created using the [trigg] function
 *
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
         * @throws IllegalStateException if there is no trigg instance available
         */
        fun getInstance(): TriggProvider =
            checkNotNull(INSTANCE) { "No trigg instance was created ! Maybe you're calling it after it got destroyed ?" }
    }

    init {
        check(INSTANCE == null) {
            "Cannot create more than 1 trigg instance at the same time !"
        }
    }

    /**
     * Contains **all** the objects created using this [TriggProvider]
     */
    val addedObjects: List<GenericGdObject>
        field: MutableList<GenericGdObject> = mutableListOf()

    /**
     * If this trigg instance has finished.
     * If set to `true` it is no longer mutable
     */
    var isFinished: Boolean = false
        internal set

    /**
     * If this trigg instance's [init] is locked
     * and so, immutable.
     *
     * This gets set to `true` as soon as this
     * gets mutated in any way
     */
    var isInitLocked: Boolean = false
        internal set

    /**
     * If this trigg instance's entrypoint has been initialized
     * @see entrypoint
     */
    var entrypointInitialized: Boolean = false
        private set

    @Suppress("PropertyName")
    internal val _triggFunctions: MutableMap<TriggFunctionType, TriggFunctionState> = mutableMapOf()
    val triggFunctions: Map<TriggFunctionType, TriggFunctionState>
        get() = this._triggFunctions

    private var currentPos: Pos = Pos.gridCentered(5f, 3f)

    private var _init: TriggInit? = null
    val init: TriggInit
        get() = this._init ?: TriggInit.DEFAULT

    private var _nextFreeGroup: UInt = 1u
    override val nextFreeGroup: UInt
        get() = this._nextFreeGroup + this.init.groupSpace.baseOffset.value

    private var _nextFreeLabel: UInt = 0u
    override val nextFreeLabel: UInt
        get() = this._nextFreeLabel + this.init.labelSpace.scriptRange.first

    private val _acquiredRegisters: SequencedSet<UInt> by lazy { LinkedHashSet(this.init.labelSpace.registers.toInt()) }
    override val acquiredRegisters: Set<UInt>
        get() = _acquiredRegisters

    override fun asRawString(): String =
        RawStringFactory.joinRawStrings(this.addedObjects)

    fun throwIfFinished() {
        check(!this.isFinished) { "Cannot mutate this TriggProvider because it has finished executing !" }
    }

    private fun lockInit() {
        this.isInitLocked = true
    }

    /**
     * Creates the initializer for this [TriggProvider]
     */
    fun init(initGetter: MutableTriggInit.() -> Unit) {
        this.throwIfFinished()
        check(!this.isInitLocked) { "Cannot initialize this trigg instance's properties after mutating the trigg instance" }
        check(this._init == null) { "Cannot set twice the init block of a TriggProvider" }

        val triggInit = MutableTriggInit(this)
        initGetter(triggInit)

        this._init = triggInit.toImmutable()
    }

    override fun <T: SimpleObject> add(obj: T): T {
        this.lockInit()
        this.throwIfFinished()
        this.addedObjects.add(obj)
        obj.groups.add(this.init.commonGroup.value)

        return obj
    }

    override fun spawn(group: Group?, delay: Float, spawner: ObjectManager<SimpleObject>.() -> Unit): SpawnTrigger {
        this.throwIfFinished()
        this.lockInit()

        val groupUInt = group?.let { group.value }
        val acquiredGroup = groupUInt ?: this.acquireGroup()

        val collector = GroupAdder(this, acquiredGroup)
        collector.spawner()

        return this.add {
            val spawnTrigger = SpawnTrigger(this.getNextPos(), acquiredGroup, delay)
            spawnTrigger.setSpawned()

            return@add spawnTrigger
        }
    }

    override fun defineFunction(triggFunction: TriggFunctionType): UInt {
        this.throwIfFinished()
        this.lockInit()

        return this.defineFunctionHelper(triggFunction, this._triggFunctions)
    }

    override fun spawnFunction(triggFunction: TriggFunctionType, canSpawnEarly: Boolean, delay: Float): SpawnTrigger {
        this.throwIfFinished()
        this.lockInit()

        return this.spawnFunctionHelper(triggFunction, canSpawnEarly, delay, this._triggFunctions)
    }

    /**
     * Spawns the group representing the [entrypoint][dsltypes.init.space.GroupSpace.entrypoint]
     */
    fun executeEntrypoint(delay: Float = 0f): SpawnTrigger =
        this.spawn(this.init.groupSpace.getEntrypoint(this), delay).apply {
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
    @OptIn(ExperimentalContracts::class)
    fun entrypoint(execute: Boolean = true, entrypointSpawner: ObjectManager<SimpleObject>.() -> Unit) {
        contract {
            callsInPlace(entrypointSpawner, InvocationKind.EXACTLY_ONCE)
        }

        this.lockInit()
        this.throwIfFinished()
        check(!this.entrypointInitialized) { "Cannot set two entrypoints" }

        this.entrypointInitialized = true
        group(this.init.groupSpace.getEntrypoint(this).toUInt(), entrypointSpawner)

        if (execute)
            this.executeEntrypoint()
    }

    /**
     * Gets the pos to place a trigger.
     * This is not needed but is more for not
     * cluttering every objects around a single point.
     *
     * If this [TriggProvider] instance has [finished executing][isFinished]
     * then only a copy of the next pos will be given. After this, no
     * more pos can be acquired
     * @return the pos where to place the next object
     */
    override fun getNextPos(): Pos {
        val posOffset = GridPos(0.1f, 1f)
        if (this.isFinished) {
            return this.currentPos + posOffset
        } else {
            this.lockInit()

            val posSnapshot = this.currentPos.copy()
            this.currentPos += posOffset
            return posSnapshot + this.init.basePos
        }
    }

    override fun acquireGroup(): UInt {
        this.lockInit()
        this.throwIfFinished()
        if (this.nextFreeGroup !in this.init.groupSpace.groupRange)
            throw IllegalStateException("Too many acquired groups. There are ${this.init.groupSpace.allocatedGroups} allocatable groups yet all of them were used")

        val acquiredGroup = this.nextFreeGroup
        this._nextFreeGroup++

        return acquiredGroup
    }

    override fun acquireLabel(): UInt {
        this.lockInit()
        this.throwIfFinished()
        if (this.nextFreeLabel !in this.init.labelSpace.scriptRange)
            throw IllegalStateException("Too many acquired labels. There are ${this.init.labelSpace.script} allocatable labels yet all of them were used")

        val acquiredLabel = this.nextFreeLabel
        this._nextFreeLabel++

        return acquiredLabel
    }

    override fun acquireRegister(): UInt {
        for (registerID in this.init.labelSpace.registerRange) {
            if (registerID !in this.acquiredRegisters) {
                this._acquiredRegisters.add(registerID)
                return registerID
            }
        }

        throw IndexOutOfBoundsException("All ${this.init.labelSpace.registers} registers were acquired ! No more registers can be acquired, maybe increase its size limit ?")
    }

    override fun releaseRegister(register: UInt) {
        require(register in this.init.labelSpace.registerRange) { "Given register $register is not in the valid register range ${this.init.labelSpace.registerRange}" }
        this._acquiredRegisters.remove(register)
    }

    @ConsistentCopyVisibility
    data class TriggFunctionState internal constructor(val group: Group, val hasBeenDefined: Boolean)
}

// TODO: Link doc in KDoc
/**
 * The first function used in any trigg script.
 * It creates a [TriggProvider] which is then used to add
 * objects, do comparisons, etc...
 */
@OptIn(ExperimentalContracts::class)
fun trigg(triggBuilder: TriggProvider.() -> Unit): TriggResult {
    contract {
        callsInPlace(triggBuilder, InvocationKind.EXACTLY_ONCE)
    }

    val trigg = TriggProvider()

    TriggProvider.INSTANCE = trigg
    triggBuilder(trigg)
    trigg.isFinished = true
    TriggProvider.INSTANCE = null

    trigg.triggFunctions
        .filter { !it.value.hasBeenDefined }
        .forEach { (function, state) ->
            throw IllegalStateException("TriggFunction $function has been called but not defined. It acquired group ${state.group}")
        }

    if (trigg.init.clearPreviousObjects)
        WSLiveEditor().use {
            it.removeObjects(trigg.init.commonGroup.value)
        }

    return TriggResult(trigg)
}
